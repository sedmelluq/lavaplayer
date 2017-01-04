package com.sedmelluq.discord.lavaplayer.container.matroska.format;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Handles reading various different EBML code formats.
 */
public class MatroskaEbmlReader {
  /**
   * Read an EBML code from data input with fixed size - no size encoded in the data.
   *
   * @param input Data input to read bytes from
   * @param codeLength Length of the code in bytes
   * @param type Method of sign handling (null is unsigned)
   * @return Read EBML code
   * @throws IOException On read error
   */
  public static long readFixedSizeEbmlInteger(DataInput input, int codeLength, Type type) throws IOException {
    long code = 0;

    for (int i = 1; i <= codeLength; i++) {
      code |= applyNextByte(codeLength, input.readByte() & 0xFF, i);
    }

    return applyType(code, codeLength, type);
  }

  /**
   * Read an EBML code from data input.
   *
   * @param input Data input to read bytes from
   * @param type Method of sign handling (null is unsigned)
   * @return Read EBML code
   * @throws IOException On read error
   */
  public static long readEbmlInteger(DataInput input, Type type) throws IOException {
    int firstByte = input.readByte() & 0xFF;
    int codeLength = getCodeLength(firstByte);

    long code = applyFirstByte(firstByte, codeLength);

    for (int i = 2; i <= codeLength; i++) {
      code |= applyNextByte(codeLength, input.readByte() & 0xFF, i);
    }

    return applyType(code, codeLength, type);
  }

  /**
   * Read an EBML code from byte buffer.
   *
   * @param buffer Buffer to read bytes from
   * @param type Method of sign handling (null is unsigned)
   * @return Read EBML code
   */
  public static long readEbmlInteger(ByteBuffer buffer, Type type) {
    int firstByte = buffer.get() & 0xFF;
    int codeLength = getCodeLength(firstByte);

    long code = applyFirstByte(firstByte, codeLength);

    for (int i = 2; i <= codeLength; i++) {
      code |= applyNextByte(codeLength, buffer.get() & 0xFF, i);
    }

    return applyType(code, codeLength, type);
  }

  private static int getCodeLength(int firstByte) {
    int codeLength = Integer.numberOfLeadingZeros(firstByte) - 23;
    if (codeLength > 8) {
      throw new IllegalStateException("More than 4 bytes for length, probably invalid data");
    }

    return codeLength;
  }

  private static long applyFirstByte(long firstByte, int codeLength) {
    return (firstByte & (0xFFL >> codeLength)) << ((codeLength - 1) << 3);
  }

  private static long applyNextByte(int codeLength, int value, int index) {
    return value << ((codeLength - index) << 3);
  }

  private static long applyType(long code, int codeLength, Type type) {
    if (type != null) {
      switch (type) {
        case SIGNED:
          return signEbmlInteger(code, codeLength);
        case LACE_SIGNED:
          return laceSignEbmlInteger(code, codeLength);
        case UNSIGNED:
        default:
          return code;
      }
    } else {
      return code;
    }
  }

  private static long laceSignEbmlInteger(long code, int codeLength) {
    switch (codeLength) {
      case 1: return code - 63;
      case 2: return code - 8191;
      case 3: return code - 1048575;
      case 4: return code - 134217727;
      default: throw new IllegalStateException("Code length out of bounds.");
    }
  }

  private static long signEbmlInteger(long code, int codeLength) {
    long mask = getSignMask(codeLength);

    if ((code & mask) != 0) {
      return code | mask;
    } else {
      return code;
    }
  }

  private static long getSignMask(int codeLength) {
    switch (codeLength) {
      case 1: return ~0x000000000000003FL;
      case 2: return ~0x0000000000001FFFL;
      case 3: return ~0x00000000000FFFFFL;
      case 4: return ~0x0000000007FFFFFFL;
      case 5: return ~0x00000003FFFFFFFFL;
      case 6: return ~0x000001FFFFFFFFFFL;
      case 7: return ~0x0000FFFFFFFFFFFFL;
      case 8: return ~0x007FFFFFFFFFFFFFL;
      default: throw new IllegalStateException("Code length out of bounds.");
    }
  }

  /**
   * EBML code type (sign handling method).
   */
  public enum Type {
    /**
     * Signed value with first bit marking the sign.
     */
    SIGNED,
    /**
     * Signed value where where sign is applied via subtraction.
     */
    LACE_SIGNED,
    /**
     * Unsigned value.
     */
    UNSIGNED
  }
}
