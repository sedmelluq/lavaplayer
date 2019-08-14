package com.sedmelluq.discord.lavaplayer.track.playback;

/**
 * Mutable audio frame which contains no dedicated buffer, but refers to a segment in a specified byte buffer.
 */
public class ReferenceMutableAudioFrame extends AbstractMutableAudioFrame {
  private byte[] frameBuffer;
  private int frameOffset;
  private int frameLength;

  /**
   * @return The underlying byte buffer.
   */
  public byte[] getFrameBuffer() {
    return frameBuffer;
  }

  /**
   * @return Offset of the frame data in the underlying byte buffer.
   */
  public int getFrameOffset() {
    return frameOffset;
  }

  /**
   * @return Offset of the end of frame data in the underlying byte buffer.
   */
  public int getFrameEndOffset() {
    return frameOffset + frameLength;
  }

  @Override
  public int getDataLength() {
    return frameLength;
  }

  @Override
  public byte[] getData() {
    byte[] data = new byte[frameLength];
    getData(data, 0);
    return data;
  }

  @Override
  public void getData(byte[] buffer, int offset) {
    System.arraycopy(frameBuffer, frameOffset, buffer, offset, frameLength);
  }

  /**
   * @param frameBuffer See {@link #getFrameBuffer()}.
   * @param frameOffset See {@link #getFrameOffset()}.
   * @param frameLength See {@link #getDataLength()}.
   */
  public void setDataReference(byte[] frameBuffer, int frameOffset, int frameLength) {
    this.frameBuffer = frameBuffer;
    this.frameOffset = frameOffset;
    this.frameLength = frameLength;
  }
}
