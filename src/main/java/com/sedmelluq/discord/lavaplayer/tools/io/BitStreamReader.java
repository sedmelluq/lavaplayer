package com.sedmelluq.discord.lavaplayer.tools.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Helper for reading a specific number of bits at a time from a stream.
 */
public class BitStreamReader {
  private final InputStream stream;
  private int currentByte;
  private int bitsLeft;

  /**
   * @param stream The underlying stream
   */
  public BitStreamReader(InputStream stream) {
    this.stream = stream;
  }

  /**
   * Get the specified number of bits as a long value
   * @param bitsNeeded Number of bits to retrieve
   * @return The value of those bits as a long
   * @throws IOException On read error
   */
  public long asLong(int bitsNeeded) throws IOException {
    long value = 0;

    while (bitsNeeded > 0) {
      fill();

      int chunk = Math.min(bitsNeeded, bitsLeft);
      int mask = (1 << chunk) - 1;

      value <<= chunk;
      value |= (currentByte >> (bitsLeft - chunk)) & mask;

      bitsNeeded -= chunk;
      bitsLeft -= chunk;
    }

    return value;
  }

  /**
   * Get the specific number of bits as a signed long value (highest order bit is sign)
   * @param bitsNeeded Number of bits needed
   * @return The signed value
   * @throws IOException On read error
   */
  public long asSignedLong(int bitsNeeded) throws IOException {
    long value = asLong(bitsNeeded);

    if ((value & (1L << (bitsNeeded - 1))) != 0) {
      return value | ~((1L << bitsNeeded) - 1);
    } else {
      return value;
    }
  }

  /**
   * Get the specified number of bits as an integer value
   * @param bitsNeeded Number of bits to retrieve
   * @return The value of those bits as an integer
   * @throws IOException On read error
   */
  public int asInteger(int bitsNeeded) throws IOException {
    return Math.toIntExact(asLong(bitsNeeded));
  }

  /**
   * Get the specific number of bits as a signed integer value (highest order bit is sign)
   * @param bitsNeeded Number of bits needed
   * @return The signed value
   * @throws IOException On read error
   */
  public int asSignedInteger(int bitsNeeded) throws IOException {
    return Math.toIntExact(asSignedLong(bitsNeeded));
  }

  /**
   * Reads bits from the stream until a set bit is reached.
   * @return The number of zeroes read
   * @throws IOException On read error
   */
  public int readAllZeroes() throws IOException {
    int count = 0;
    fill();

    while ((currentByte & (1 << --bitsLeft)) == 0) {
      count++;
      fill();
    }

    return count;
  }

  /**
   * Reads the number of bits it requires to make the reader align on a byte.
   * @return The read bits as an unsigned value
   */
  public int readRemainingBits() {
    int value = currentByte & ((1 << bitsLeft) - 1);
    bitsLeft = 0;
    return value;
  }

  private void fill() throws IOException {
    if (bitsLeft == 0) {
      currentByte = readByte();
      bitsLeft = 8;

      if (currentByte == -1) {
        throw new EOFException("Bit stream needs more bytes");
      }
    }
  }

  protected int readByte() throws IOException {
    return stream.read();
  }
}
