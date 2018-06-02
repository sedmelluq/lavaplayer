package com.sedmelluq.discord.lavaplayer.track.playback;

import java.nio.ByteBuffer;

/**
 * A mutable audio frame.
 */
public class MutableAudioFrame extends AbstractMutableAudioFrame {
  private ByteBuffer frameBuffer;
  private int overrideBufferPosition;

  /**
   * This should be called only by the requester of a frame.
   *
   * @param frameBuffer Buffer to use internally.
   */
  public void setBuffer(ByteBuffer frameBuffer) {
    this.frameBuffer = frameBuffer;
    this.overrideBufferPosition = frameBuffer.position();
  }

  /**
   * This should be called only by the provider of a frame.
   *
   * @param buffer Buffer to copy data from into the internal buffer of this instance.
   * @param offset Offset in the buffer.
   * @param length Length of the data to copy.
   */
  public void store(byte[] buffer, int offset, int length) {
    frameBuffer.position(overrideBufferPosition);
    frameBuffer.put(buffer, offset, length);
  }

  @Override
  public int getDataLength() {
    return frameBuffer.remaining();
  }

  @Override
  public byte[] getData() {
    byte[] data = new byte[getDataLength()];
    getData(data, 0);
    return data;
  }

  @Override
  public void getData(byte[] buffer, int offset) {
    frameBuffer.mark();
    frameBuffer.get(buffer, offset, frameBuffer.remaining());
    frameBuffer.reset();
  }
}
