package com.sedmelluq.discord.lavaplayer.track.playback;

import java.nio.ByteBuffer;

/**
 * A mutable audio frame.
 */
public class MutableAudioFrame extends AbstractMutableAudioFrame {
  private ByteBuffer frameBuffer;
  private int framePosition;
  private int frameLength;

  /**
   * This should be called only by the requester of a frame.
   *
   * @param frameBuffer Buffer to use internally.
   */
  public void setBuffer(ByteBuffer frameBuffer) {
    this.frameBuffer = frameBuffer;
    this.framePosition = frameBuffer.position();
    this.frameLength = frameBuffer.remaining();
  }

  /**
   * This should be called only by the provider of a frame.
   *
   * @param buffer Buffer to copy data from into the internal buffer of this instance.
   * @param offset Offset in the buffer.
   * @param length Length of the data to copy.
   */
  public void store(byte[] buffer, int offset, int length) {
    frameBuffer.position(framePosition);
    frameBuffer.limit(frameBuffer.capacity());
    frameBuffer.put(buffer, offset, length);
    frameLength = length;
  }

  @Override
  public int getDataLength() {
    return frameLength;
  }

  @Override
  public byte[] getData() {
    byte[] data = new byte[getDataLength()];
    getData(data, 0);
    return data;
  }

  @Override
  public void getData(byte[] buffer, int offset) {
    int previous = frameBuffer.position();
    frameBuffer.position(framePosition);
    frameBuffer.get(buffer, offset, frameLength);
    frameBuffer.position(previous);
  }
}
