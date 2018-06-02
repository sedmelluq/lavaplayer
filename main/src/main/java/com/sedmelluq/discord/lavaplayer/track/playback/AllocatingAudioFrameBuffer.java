package com.sedmelluq.discord.lavaplayer.track.playback;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A frame buffer. Stores the specified duration worth of frames in the internal buffer.
 * Consumes frames in a blocking manner and provides frames in a non-blocking manner.
 */
public class AllocatingAudioFrameBuffer extends AbstractAudioFrameBuffer {
  private static final Logger log = LoggerFactory.getLogger(AudioFrameBuffer.class);

  private final int fullCapacity;
  private final ArrayBlockingQueue<AudioFrame> audioFrames;
  private final AtomicBoolean stopping;

  /**
   * @param bufferDuration The length of the internal buffer in milliseconds
   * @param format The format of the frames held in this buffer
   * @param stopping Atomic boolean which has true value when the track is in a state of pending stop.
   */
  public AllocatingAudioFrameBuffer(int bufferDuration, AudioDataFormat format, AtomicBoolean stopping) {
    super(format);
    this.fullCapacity = bufferDuration / 20 + 1;
    this.audioFrames = new ArrayBlockingQueue<>(fullCapacity);
    this.stopping = stopping;
  }

  /**
   * @return Number of frames that can be added to the buffer without blocking.
   */
  @Override
  public int getRemainingCapacity() {
    return audioFrames.remainingCapacity();
  }

  /**
   * @return Total number of frames that the buffer can hold.
   */
  @Override
  public int getFullCapacity() {
    return fullCapacity;
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

      if (timeout > 0) {
        frame = audioFrames.poll(timeout, unit);
        terminator = fetchPendingTerminator();

        if (terminator != null) {
          return terminator;
        }
      }
    }

    return filterFrame(frame);
  }

  @Override
  public boolean provide(MutableAudioFrame targetFrame) {
    return passToMutable(provide(), targetFrame);
  }

  @Override
  public boolean provide(MutableAudioFrame targetFrame, long timeout, TimeUnit unit)
      throws TimeoutException, InterruptedException {

    return passToMutable(provide(timeout, unit), targetFrame);
  }

  private boolean passToMutable(AudioFrame frame, MutableAudioFrame targetFrame) {
    if (targetFrame != null) {
      if (frame.isTerminator()) {
        targetFrame.setTerminator(true);
      } else {
        targetFrame.setTimecode(frame.getTimecode());
        targetFrame.setVolume(frame.getVolume());
        targetFrame.store(frame.getData(), 0, frame.getDataLength());
        targetFrame.setTerminator(false);
      }

      return true;
    }

    return false;
  }

  @Override
  public void clear() {
    audioFrames.clear();
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

  /**
   * @return The timecode of the last frame in the buffer, null if the buffer is empty or is marked to be cleared upon
   *         receiving the next frame.
   */
  @Override
  public Long getLastInputTimecode() {
    Long lastTimecode = null;

    synchronized (synchronizer) {
      if (!clearOnInsert) {
        for (AudioFrame frame : audioFrames) {
          lastTimecode = frame.getTimecode();
        }
      }
    }

    return lastTimecode;
  }

  @Override
  public void consume(AudioFrame frame) throws InterruptedException {
    // If an interrupt sent along with setting the stopping status was silently consumed elsewhere, this check should
    // still trigger. Guarantees that stopped tracks cannot get stuck in this method. Possible performance improvement:
    // offer with timeout, check stopping if timed out, then put?
    if (stopping != null && stopping.get()) {
      throw new InterruptedException();
    }

    if (!locked) {
      receivedFrames = true;

      if (clearOnInsert) {
        audioFrames.clear();
        clearOnInsert = false;
      }

      if (frame instanceof AbstractMutableAudioFrame) {
        frame = ((AbstractMutableAudioFrame) frame).freeze();
      }

      audioFrames.put(frame);
    }
  }

  private AudioFrame fetchPendingTerminator() {
    synchronized (synchronizer) {
      if (terminateOnEmpty) {
        terminateOnEmpty = false;
        terminated = true;
        synchronizer.notifyAll();
        return TerminatorAudioFrame.INSTANCE;
      }
    }

    return null;
  }

  private AudioFrame filterFrame(AudioFrame frame) {
    if (frame != null && frame.getVolume() == 0) {
      return new ImmutableAudioFrame(frame.getTimecode(), format.silenceBytes(), 0, format);
    }

    return frame;
  }
}
