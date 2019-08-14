package com.sedmelluq.discord.lavaplayer.tools.io;

import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.io.IOUtils.EOF;

/**
 * Bounded input stream where the limit can be set dynamically.
 */
public class ResettableBoundedInputStream extends InputStream {
  private final InputStream delegate;

  private long limit;
  private long position;

  /**
   * @param delegate Underlying input stream.
   */
  public ResettableBoundedInputStream(InputStream delegate) {
    this.delegate = delegate;
    this.limit = Long.MAX_VALUE;
    this.position = 0;
  }

  /**
   * Make this input stream return EOF after the specified number of bytes.
   * @param limit Maximum number of bytes that can be read.
   */
  public void resetLimit(long limit) {
    this.position = 0;
    this.limit = limit;
  }

  @Override
  public int read() throws IOException {
    if (position >= limit) {
      return -1;
    }

    int result = delegate.read();
    if (result != -1) {
      position++;
    }

    return result;
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    if (position >= limit) {
      return EOF;
    }

    int chunk = (int) Math.min(length, limit - position);
    int read = delegate.read(buffer, offset, chunk);

    if (read == -1) {
      return -1;
    }

    position += read;
    return read;
  }

  @Override
  public long skip(final long distance) throws IOException {
    int chunk = (int) Math.min(distance, limit - position);
    long skipped = delegate.skip(chunk);
    position += skipped;
    return skipped;
  }

  @Override
  public int available() throws IOException {
    return (int) Math.min(limit - position, delegate.available());
  }

  @Override
  public void close() throws IOException {
    // Nothing to do
  }

  @Override
  public boolean markSupported() {
    return false;
  }
}
