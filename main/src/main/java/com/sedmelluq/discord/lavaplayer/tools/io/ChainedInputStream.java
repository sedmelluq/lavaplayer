package com.sedmelluq.discord.lavaplayer.tools.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Input stream which can swap the underlying input stream if the current one ends.
 */
public class ChainedInputStream extends InputStream {
  private final Provider provider;
  private InputStream currentStream;
  private boolean streamEnded;

  /**
   * @param provider Provider for input streams to chain.
   */
  public ChainedInputStream(Provider provider) {
    this.provider = provider;
  }

  private boolean loadNextStream() throws IOException {
    if (!streamEnded) {
      close();

      currentStream = provider.next();

      if (currentStream == null) {
        streamEnded = true;
      }
    }

    return !streamEnded;
  }

  @Override
  public int read() throws IOException {
    if (streamEnded || (currentStream == null && !loadNextStream())) {
      return -1;
    }

    int result;
    int emptyStreamCount = 0;

    while ((result = currentStream.read()) == -1 && ++emptyStreamCount < 5) {
      if (!loadNextStream()) {
        return -1;
      }
    }

    return result;
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    if (streamEnded || (currentStream == null && !loadNextStream())) {
      return -1;
    }

    int result;
    int emptyStreamCount = 0;

    while ((result = currentStream.read(buffer, offset, length)) == -1 && ++emptyStreamCount < 5) {
      if (!loadNextStream()) {
        return -1;
      }
    }

    return result;
  }

  @Override
  public long skip(long distance) throws IOException {
    if (streamEnded || (currentStream == null && !loadNextStream())) {
      return -1;
    }

    long result;
    int emptyStreamCount = 0;

    while ((result = currentStream.skip(distance)) == 0 && ++emptyStreamCount < 5) {
      if (!loadNextStream()) {
        return 0;
      }
    }

    return result;
  }

  @Override
  public void close() throws IOException {
    if (currentStream != null) {
      currentStream.close();
      currentStream = null;
    }
  }

  @Override
  public boolean markSupported() {
    return false;
  }

  /**
   * Provider for next input stream of a chained stream.
   */
  public interface Provider {
    /**
     * @return Next input stream, null to cause EOF on the chained stream.
     * @throws IOException On read error.
     */
    InputStream next() throws IOException;
  }
}
