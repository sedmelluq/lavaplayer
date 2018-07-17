package com.sedmelluq.discord.lavaplayer.container.matroska.format;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * An implementation of {@link MatroskaBlock} which can be reused by loading the next block into it by calling
 * {@link #parseHeader(MatroskaFileReader, MatroskaElement, int)}. Does not reallocate any objects unless it encounters
 * a block with more than twice as many frames as seen before, or a frame more than twice as long as before.
 */
public class MutableMatroskaBlock implements MatroskaBlock {
  private int timecode;
  private int trackNumber;
  private boolean keyFrame;
  private int[] frameSizes;
  private int frameCount;
  private ByteBuffer buffer;
  private byte[] bufferArray;

  @Override
  public int getTimecode() {
    return timecode;
  }

  @Override
  public int getTrackNumber() {
    return trackNumber;
  }

  @Override
  public boolean isKeyFrame() {
    return keyFrame;
  }

  @Override
  public int getFrameCount() {
    return frameCount;
  }

  @Override
  public ByteBuffer getNextFrameBuffer(MatroskaFileReader reader, int index) throws IOException {
    if (index >= frameCount) {
      throw new IllegalArgumentException("Frame index out of bounds.");
    }

    int frameSize = frameSizes[index];

    if (buffer == null || frameSize > buffer.capacity()) {
      buffer = ByteBuffer.allocate(frameSizes[index] * 2);
      bufferArray = buffer.array();
    }

    reader.getDataInput().readFully(bufferArray, 0, frameSize);

    buffer.position(0);
    buffer.limit(frameSize);
    return buffer;
  }

  /**
   * Parses the Matroska block header data into the fields of this instance. On success of this method, this instance
   * effectively represents that block.
   *
   * @param reader The reader to use.
   * @param element The block EBML element.
   * @param trackFilter The ID of the track to read data for from the block.
   * @return <code>true</code> of a block if it contains data for the requested track, <code>false</code> otherwise.
   * @throws IOException On read error.
   */
  public boolean parseHeader(MatroskaFileReader reader, MatroskaElement element, int trackFilter) throws IOException {
    DataInput input = reader.getDataInput();
    trackNumber = (int) MatroskaEbmlReader.readEbmlInteger(input, null);

    if (trackFilter >= 0 && trackNumber != trackFilter) {
      return false;
    }

    timecode = input.readShort();

    int flags = input.readByte() & 0xFF;
    keyFrame = (flags & 0x80) != 0;

    int laceType = (flags & 0x06) >> 1;

    if (laceType != 0) {
      setFrameCount((input.readByte() & 0xFF) + 1);
      parseLacing(reader, element, laceType);
    } else {
      setFrameCount(1);
      frameSizes[0] = (int) element.getRemaining(reader.getPosition());
    }

    return true;
  }

  private void parseLacing(MatroskaFileReader reader, MatroskaElement element, int laceType) throws IOException {
    setFrameCount(frameCount);

    switch (laceType) {
      case 1:
        parseXiphLaceSizes(reader, element);
        break;
      case 2:
        parseFixedLaceSizes(reader, element);
        break;
      case 3:
      default:
        parseEbmlLaceSizes(reader, element);
    }
  }

  private void setFrameCount(int frameCount) {
    if (frameSizes == null || frameSizes.length < frameCount) {
      frameSizes = new int[frameCount * 2];
    }

    this.frameCount = frameCount;
  }

  private void parseXiphLaceSizes(MatroskaFileReader reader, MatroskaElement element) throws IOException {
    int sizeTotal = 0;
    DataInput input = reader.getDataInput();

    for (int i = 0; i < frameCount - 1; i++) {
      int value;

      do {
        value = input.readByte() & 0xFF;
        frameSizes[i] += value;
      } while (value == 255);

      sizeTotal += frameSizes[i];
    }

    frameSizes[frameCount - 1] = (int) element.getRemaining(reader.getPosition()) - sizeTotal;
  }

  private void parseFixedLaceSizes(MatroskaFileReader reader, MatroskaElement element) {
    int size = (int) element.getRemaining(reader.getPosition()) / frameCount;

    for (int i = 0; i < frameCount; i++) {
      frameSizes[i] = size;
    }
  }

  private void parseEbmlLaceSizes(MatroskaFileReader reader, MatroskaElement element) throws IOException {
    DataInput input = reader.getDataInput();

    frameSizes[0] = (int) MatroskaEbmlReader.readEbmlInteger(input, null);
    int sizeTotal = frameSizes[0];

    for (int i = 1; i < frameCount - 1; i++) {
      frameSizes[i] = frameSizes[i - 1] + (int) MatroskaEbmlReader.readEbmlInteger(input, MatroskaEbmlReader.Type.LACE_SIGNED);
      sizeTotal += frameSizes[i];
    }

    frameSizes[frameCount - 1] = (int) element.getRemaining(reader.getPosition()) - sizeTotal;
  }
}
