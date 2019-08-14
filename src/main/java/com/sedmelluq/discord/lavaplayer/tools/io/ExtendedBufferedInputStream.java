package com.sedmelluq.discord.lavaplayer.tools.io;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * A buffered input stream that gives with the ability to get the number of bytes left in the buffer and a method for
 * discarding the buffer.
 */
public class ExtendedBufferedInputStream extends BufferedInputStream {
  /**
   * @param in Underlying input stream
   */
  public ExtendedBufferedInputStream(InputStream in) {
    super(in);
  }

  /**
   * @param in Underlying input stream
   * @param size Size of the buffer
   */
  public ExtendedBufferedInputStream(InputStream in, int size) {
    super(in, size);
  }

  /**
   * @return The number of bytes left in the buffer. This is useful for calculating the actual position in the buffer
   *         if the position in the underlying buffer is known.
   */
  public int getBufferedByteCount() {
    return count - pos;
  }

  /**
   * Discard the remaining buffer. This should be called after seek has been performed on the underlying stream.
   */
  public void discardBuffer() {
    pos = count;
  }
}
