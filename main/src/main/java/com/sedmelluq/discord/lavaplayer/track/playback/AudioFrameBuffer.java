package com.sedmelluq.discord.lavaplayer.track.playback;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A frame buffer. Stores the specified duration worth of frames in the internal buffer.
 * Consumes frames in a blocking manner and provides frames in a non-blocking manner.
 */
public class AudioFrameBuffer implements AudioFrameConsumer, AudioFrameProvider {
  private static final Logger log = LoggerFactory.getLogger(AudioFrameBuffer.class);

  private final Object synchronizer;
  private final int fullCapacity;
  private final ArrayBlockingQueue<AudioFrame> audioFrames;
  private final AudioDataFormat format;
  private volatile boolean locked;
  private volatile boolean receivedFrames;
  private boolean terminated;
  private boolean terminateOnEmpty;
  private boolean clearOnInsert;

  /**
   * @param bufferDuration The length of the internal buffer in milliseconds
   * @param format The format of the frames held in this buffer
   */
  public AudioFrameBuffer(int bufferDuration, AudioDataFormat format) {
    synchronizer = new Object();
    fullCapacity = bufferDuration / 20 + 1;
    audioFrames = new ArrayBlockingQueue<>(fullCapacity);
    this.format = format;
    terminated = false;
    terminateOnEmpty = false;
    clearOnInsert = false;
    receivedFrames = false;
  }

  @Override
  public void consume(AudioFrame frame) throws InterruptedException {
    if (!locked) {
      receivedFrames = true;

      if (clearOnInsert) {
        audioFrames.clear();
        clearOnInsert = false;
      }

      audioFrames.put(frame);
    }
  }

  /**
   * @return Number of frames that can be added to the buffer without blocking.
   */
  public int getRemainingCapacity() {
    return audioFrames.remainingCapacity();
  }

  /**
   * @return Total number of frames that the buffer can hold.
   */
  public int getFullCapacity() {
    return fullCapacity;
  }

  /**
   * Wait until another thread has consumed a terminator frame from this buffer
   * @throws InterruptedException When interrupted, expected on seek or stop
   */
  public void waitForTermination() throws InterruptedException {
    synchronized (synchronizer) {
      while (!terminated) {
        synchronizer.wait();
      }
    }
  }

  @Override
  public AudioFrame provide() {
    AudioFrame frame = audioFrames.poll();

    if (frame == null) {
      return fetchPendingTerminator();
    }

    return filterFrame(frame);
  }

  @Override
  public AudioFrame provide(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
    AudioFrame frame = audioFrames.poll();

    if (frame == null) {
      AudioFrame terminator = fetchPendingTerminator();
      if (terminator != null) {
        return terminator;
      }

      frame = audioFrames.poll(timeout, unit);
      terminator = fetchPendingTerminator();

      if (terminator != null) {
        return terminator;
      }
    }

    return filterFrame(frame);
  }

  private AudioFrame fetchPendingTerminator() {
    synchronized (synchronizer) {
      if (terminateOnEmpty) {
        terminateOnEmpty = false;
        terminated = true;
        synchronizer.notifyAll();
        return AudioFrame.TERMINATOR;
      }
    }

    return null;
  }

  private AudioFrame filterFrame(AudioFrame frame) {
    if (frame != null && frame.volume == 0) {
      return new AudioFrame(frame.timecode, format.silence, 0, format);
    }

    return frame;
  }

  /**
   * Signal that no more input is expected and if the content frames have been consumed, emit a terminator frame.
   */
  public void setTerminateOnEmpty() {
    synchronized (synchronizer) {
      // Count this also as inserting the terminator frame, hence trigger clearOnInsert
      if (clearOnInsert) {
        audioFrames.clear();
        clearOnInsert = false;
      }

      if (!terminated) {
        terminateOnEmpty = true;
      }
    }
  }

  /**
   * Signal that the next frame provided to the buffer will clear the frames before it. This is useful when the next
   * data is not contiguous with the current frame buffer, but the remaining frames in the buffer should be used until
   * the next data arrives to prevent a situation where the buffer cannot provide any frames for a while.
   */
  public void setClearOnInsert() {
    synchronized (synchronizer) {
      clearOnInsert = true;
      terminateOnEmpty = false;
    }
  }

  /**
   * @return Whether the next frame is set to clear the buffer.
   */
  public boolean hasClearOnInsert() {
    return clearOnInsert;
  }

  /**
   * Clear the buffer.
   */
  public void clear() {
    audioFrames.clear();
  }

  /**
   * Lock the buffer so no more incoming frames are accepted.
   */
  public void lockBuffer() {
    locked = true;
  }

  /**
   * @return True if this buffer has received any input frames.
   */
  public boolean hasReceivedFrames() {
    return receivedFrames;
  }

  @Override
  public void rebuild(AudioFrameRebuilder rebuilder) {
    List<AudioFrame> frames = new ArrayList<>();
    int frameCount = audioFrames.drainTo(frames);

    log.debug("Running rebuilder {} on {} buffered frames.", rebuilder.getClass().getSimpleName(), frameCount);

    for (AudioFrame frame : frames) {
      audioFrames.add(rebuilder.rebuild(frame));
    }
  }
}
