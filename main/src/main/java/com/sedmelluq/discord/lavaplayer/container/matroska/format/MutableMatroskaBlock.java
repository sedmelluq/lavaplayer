package com.sedmelluq.discord.lavaplayer.container.matroska.format;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MutableMatroskaBlock implements MatroskaBlock {
  private long startPosition;
  private int timecode;
  private int trackNumber;
  private boolean keyFrame;
  private int[] frameOffsets;
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
      frameOffsets[0] = 0;
    }

    startPosition = reader.getPosition();
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

    frameOffsets[0] = 0;

    for (int i = 1; i < frameSizes.length; i++) {
      frameOffsets[i] = frameOffsets[i - 1] + frameSizes[i - 1];
    }
  }

  private void setFrameCount(int frameCount) {
    if (frameSizes == null || frameSizes.length < frameCount) {
      frameSizes = new int[frameCount * 2];
      frameOffsets = new int[frameCount * 2];
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
