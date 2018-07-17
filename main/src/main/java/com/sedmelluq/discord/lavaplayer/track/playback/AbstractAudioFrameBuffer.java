package com.sedmelluq.discord.lavaplayer.track.playback;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;

/**
 * Common parts of a frame buffer which are not likely to depend on the specific implementation.
 */
public abstract class AbstractAudioFrameBuffer implements AudioFrameBuffer {
  protected final AudioDataFormat format;
  protected final Object synchronizer;
  protected volatile boolean locked;
  protected volatile boolean receivedFrames;
  protected boolean terminated;
  protected boolean terminateOnEmpty;
  protected boolean clearOnInsert;

  protected AbstractAudioFrameBuffer(AudioDataFormat format) {
    this.format = format;
    this.synchronizer = new Object();
    locked = false;
    receivedFrames = false;
    terminated = false;
    terminateOnEmpty = false;
    clearOnInsert = false;
  }

  @Override
  public void waitForTermination() throws InterruptedException {
    synchronized (synchronizer) {
      while (!terminated) {
        synchronizer.wait();
      }
    }
  }

  @Override
  public void setTerminateOnEmpty() {
    synchronized (synchronizer) {
      // Count this also as inserting the terminator frame, hence trigger clearOnInsert
      if (clearOnInsert) {
        clear();
        clearOnInsert = false;
      }

      if (!terminated) {
        terminateOnEmpty = true;
      }
    }
  }

  @Override
  public void setClearOnInsert() {
    synchronized (synchronizer) {
      clearOnInsert = true;
      terminateOnEmpty = false;
    }
  }

  @Override
  public boolean hasClearOnInsert() {
    return clearOnInsert;
  }

  @Override
  public void lockBuffer() {
    locked = true;
  }

  @Override
  public boolean hasReceivedFrames() {
    return receivedFrames;
  }
}
