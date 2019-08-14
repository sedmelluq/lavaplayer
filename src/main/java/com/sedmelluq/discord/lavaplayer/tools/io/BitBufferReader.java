package com.sedmelluq.discord.lavaplayer.tools.io;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Helper for reading a specific number of bits at a time from a byte buffer.
 */
public class BitBufferReader extends BitStreamReader {
  private final ByteBuffer buffer;

  /**
   * @param buffer Byte buffer to read bytes from
   */
  public BitBufferReader(ByteBuffer buffer) {
    super(null);

    this.buffer = buffer;
  }

  @Override
  public long asLong(int bitsNeeded) {
    try {
      return super.asLong(bitsNeeded);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int asInteger(int bitsNeeded) {
    try {
      return super.asInteger(bitsNeeded);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected int readByte() throws IOException {
    return buffer.get() & 0xFF;
  }
}
