package com.sedmelluq.discord.lavaplayer.tools.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Input stream wrapper which reads or skips until EOF or requested length.
 */
public class GreedyInputStream extends FilterInputStream {
  /**
   * @param in Underlying input stream.
   */
  public GreedyInputStream(InputStream in) {
    super(in);
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    int read = 0;

    while (read < length) {
      int chunk = in.read(buffer, offset + read, length - read);
      if (chunk == -1) {
        return read == 0 ? -1 : read;
      }
      read += chunk;
    }

    return read;
  }

  @Override
  public long skip(long maximum) throws IOException {
    long skipped = 0;

    while (skipped < maximum) {
      long chunk = in.skip(maximum - skipped);
      if (chunk == 0) {
        if (in.read() == -1) {
          break;
        } else {
          chunk = 1;
        }
      }
      skipped += chunk;
    }

    return skipped;
  }
}
