package com.sedmelluq.discord.lavaplayer.container.matroska;

import java.nio.ByteBuffer;

/**
 * Provides information about a block and buffers for its individual frames.
 */
public class MatroskaFixedBlock {
  private final ByteBuffer buffer;

  private int startPosition;
  private int timecode;
  private int trackNumber;
  private boolean keyFrame;
  private int[] offsets;
  private int[] sizes;

  /**
   * @param buffer The buffer for this block
   */
  public MatroskaFixedBlock(ByteBuffer buffer) {
    this.buffer = buffer;
  }

  /**
   * @return The timecode of this block relative to its cluster
   */
  public int getTimecode() {
    return timecode;
  }

  /**
   * @return The track number which this block is for
   */
  public int getTrackNumber() {
    return trackNumber;
  }

  /**
   * @return Whether this block is a keyframe
   */
  public boolean isKeyFrame() {
    return keyFrame;
  }

  /**
   * @return The number of frames in this block
   */
  public int getFrameCount() {
    return sizes == null ? 1 : sizes.length;
  }

  /**
   * Parse the header of this block to initialise its fields
   */
  public void parseHeader() {
    trackNumber = readEbmlCode(false);
    timecode = buffer.getShort();

    int flags = buffer.get() & 0xFF;
    keyFrame = (flags & 0x80) != 0;

    int laceType = (flags & 0x06) >> 1;

    if (laceType != 0) {
      parseLacing(laceType, buffer.get() & 0xFF);
    }

    startPosition = buffer.position();
  }

  /**
   * @param index Frame index
   * @return Buffer for the specified frame
   */
  public ByteBuffer getFrameBuffer(int index) {
    ByteBuffer frameBuffer = buffer.duplicate();

    if (sizes == null) {
      if (index > 0) {
        throw new IllegalArgumentException("Frame index out of bounds.");
      }

      frameBuffer.limit(buffer.limit());
      frameBuffer.position(startPosition);
    } else {
      if (index >= sizes.length) {
        throw new IllegalArgumentException("Frame index out of bounds.");
      }

      frameBuffer.limit(offsets[index] + sizes[index]);
      frameBuffer.position(offsets[index]);
    }

    return frameBuffer;
  }

  private void parseLacing(int laceType, int length) {
    sizes = new int[length + 1];
    offsets = new int[length + 1];

    switch (laceType) {
      case 1:
        parseXiphLaceSizes(sizes);
        break;
      case 2:
        parseFixedLaceSizes(sizes);
        break;
      case 3:
      default:
        parseEbmlLaceSizes(sizes);
    }

    offsets[0] = buffer.position();

    for (int i = 1; i < sizes.length; i++) {
      offsets[i] = offsets[i - 1] + sizes[i - 1];
    }
  }

  private void parseXiphLaceSizes(int[] laceSizes) {
    int sizeTotal = 0;

    for (int i = 0; i < laceSizes.length - 1; i++) {
      int value;

      do {
        value = buffer.get() & 0xFF;
        laceSizes[i] += value;
      } while (value == 255);

      sizeTotal += laceSizes[i];
    }

    laceSizes[laceSizes.length - 1] = buffer.remaining() - sizeTotal;
  }

  private void parseFixedLaceSizes(int[] laceSizes) {
    int size = buffer.remaining() / laceSizes.length;

    for (int i = 0; i < laceSizes.length; i++) {
      laceSizes[i] = size;
    }
  }

  private void parseEbmlLaceSizes(int[] laceSizes) {
    laceSizes[0] = readEbmlCode(false);
    int sizeTotal = laceSizes[0];

    for (int i = 1; i < laceSizes.length - 1; i++) {
      laceSizes[i] = laceSizes[i - 1] + readEbmlCode(true);
      sizeTotal += laceSizes[i];
    }

    laceSizes[laceSizes.length - 1] = buffer.remaining() - sizeTotal;
  }

  private int readEbmlCode(boolean signed) {
    int firstByte = buffer.get() & 0xFF;

    int codeLength = Integer.numberOfLeadingZeros(firstByte) - 23;
    if (codeLength > 4) {
      throw new IllegalStateException("More than 4 bytes for length, probably invalid data");
    }

    int code = (firstByte & (0xFF >> codeLength)) << ((codeLength - 1) << 3);

    for (int i = 2; i <= codeLength; i++) {
      code |= (buffer.get() & 0xFF) << ((codeLength - i) << 3);
    }

    if (signed) {
      return signEbmlCode(code, codeLength);
    } else {
      return code;
    }
  }

  private int signEbmlCode(int code, int codeLength) {
    switch (codeLength) {
      case 1: return code - 63;
      case 2: return code - 8191;
      case 3: return code - 1048575;
      case 4: return code - 134217727;
      default: return code;
    }
  }
}
