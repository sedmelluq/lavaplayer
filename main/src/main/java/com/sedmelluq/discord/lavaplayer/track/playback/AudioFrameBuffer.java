package com.sedmelluq.discord.lavaplayer.track.playback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A frame buffer. Stores the specified duration worth of frames in the internal buffer.
 * Consumes frames in a blocking manner and provides frames in a non-blocking manner.
 */
public class AudioFrameBuffer implements AudioFrameConsumer, AudioFrameProvider {
  private static final Logger log = LoggerFactory.getLogger(AudioFrameBuffer.class);

  private static final byte[] SILENT_OPUS_FRAME = new byte[] {(byte) 0xFC, (byte) 0xFF, (byte) 0xFE};

  private final Object synchronizer;
  private final BlockingQueue<AudioFrame> audioFrames;
  private boolean terminated;
  private boolean terminateOnEmpty;
  private boolean clearOnInsert;

  /**
   * @param bufferDuration The length of the internal buffer in milliseconds
   */
  public AudioFrameBuffer(int bufferDuration) {
    synchronizer = new Object();
    audioFrames = new ArrayBlockingQueue<>(bufferDuration / 20 + 1);
    terminated = false;
    terminateOnEmpty = false;
    clearOnInsert = false;
  }

  @Override
  public void consume(AudioFrame frame) throws InterruptedException {
    if (clearOnInsert) {
      audioFrames.clear();
      clearOnInsert = false;
    }

    audioFrames.put(frame);
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
      synchronized (synchronizer) {
        if (terminateOnEmpty) {
          terminateOnEmpty = false;
          terminated = true;
          synchronizer.notifyAll();
          return AudioFrame.TERMINATOR;
        }
      }
    } else if (frame.volume == 0) {
      return new AudioFrame(frame.timecode, SILENT_OPUS_FRAME, 0);
    }

    return frame;
  }

  /**
   * Signal that no more input is expected and if the content frames have been consumed, emit a terminator frame.
   */
  public void setTerminateOnEmpty() {
    synchronized (synchronizer) {
      if (!terminated) {
        terminateOnEmpty = true;
      }
    }
  }

  /**
   * Signal that the next frame provided to the buffer will clear the frames before it.
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
