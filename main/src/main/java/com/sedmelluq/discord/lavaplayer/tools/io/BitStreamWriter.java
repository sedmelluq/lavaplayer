package com.sedmelluq.discord.lavaplayer.tools.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Helper for writing a specific number of bits at a time to a stream.
 */
public class BitStreamWriter {
  private final OutputStream stream;
  private int currentByte;
  private int bitsUnused;

  /**
   * @param stream The underlying stream
   */
  public BitStreamWriter(OutputStream stream) {
    this.stream = stream;
    bitsUnused = 8;
  }

  /**
   * @param value The value to take the bits from (lower order bits first)
   * @param bits Number of bits to write
   * @throws IOException On write error
   */
  public void write(long value, int bits) throws IOException {
    int bitsToPush = bits;

    while (bitsToPush > 0) {
      int chunk = Math.min(bitsUnused, bitsToPush);
      int mask = (1 << chunk) - 1;

      currentByte |= (((int) (value >> (bitsToPush - chunk))) & mask) << (bitsUnused - chunk);

      sendOnFullByte();

      bitsToPush -= chunk;
      bitsUnused -= chunk;
    }
  }

  private void sendOnFullByte() throws IOException {
    if (bitsUnused == 0) {
      stream.write(currentByte);
      bitsUnused = 8;
      currentByte = 0;
    }
  }

  /**
   * Flush the current byte even if there are remaining unused bits left
   * @throws IOException On write error
   */
  public void flush() throws IOException {
    if (bitsUnused < 8) {
      stream.write(currentByte);
    }

    bitsUnused = 8;
    currentByte = 0;
  }
}
