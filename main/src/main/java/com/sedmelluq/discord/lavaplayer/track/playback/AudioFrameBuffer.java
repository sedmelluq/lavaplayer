package com.sedmelluq.discord.lavaplayer.track.playback;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A frame buffer. Stores the specified duration worth of frames in the internal buffer.
 * Consumes frames in a blocking manner and provides frames in a non-blocking manner.
 */
public class AudioFrameBuffer implements AudioFrameConsumer, AudioFrameProvider {
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
}
