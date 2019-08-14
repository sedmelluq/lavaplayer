package com.sedmelluq.discord.lavaplayer.tools.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * A byte buffer exposed as an input stream.
 */
public class ByteBufferInputStream extends InputStream {
  private final ByteBuffer buffer;

  /**
   * @param buffer The buffer to read from.
   */
  public ByteBufferInputStream(ByteBuffer buffer) {
    this.buffer = buffer;
  }

  @Override
  public int read() throws IOException {
    if (buffer.hasRemaining()) {
      return buffer.get() & 0xFF;
    } else {
      return -1;
    }
  }

  @Override
  public int read(byte[] array, int offset, int length) throws IOException {
    if (buffer.hasRemaining()) {
      int chunk = Math.min(buffer.remaining(), length);
      buffer.get(array, offset, length);
      return chunk;
    } else {
      return -1;
    }
  }

  @Override
  public int available() throws IOException {
    return buffer.remaining();
  }
}
