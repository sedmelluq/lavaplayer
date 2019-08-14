package com.sedmelluq.discord.lavaplayer.tools.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * A byte buffer wrapped in an output stream.
 */
public class ByteBufferOutputStream extends OutputStream {
  private final ByteBuffer buffer;

  /**
   * @param buffer The underlying byte buffer
   */
  public ByteBufferOutputStream(ByteBuffer buffer) {
    this.buffer = buffer;
  }

  @Override
  public void write(int b) throws IOException {
    buffer.put((byte) b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    buffer.put(b, off, len);
  }
}
