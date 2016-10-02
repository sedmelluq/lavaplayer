package com.sedmelluq.discord.lavaplayer.tools.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream that is seekable.
 */
public abstract class SeekableInputStream extends InputStream {
  protected final long contentLength;
  private final long maxSkipDistance;

  /**
   * @param contentLength Total stream length
   * @param maxSkipDistance Maximum distance that should be skipped by reading and discarding
   */
  public SeekableInputStream(long contentLength, long maxSkipDistance) {
    this.contentLength = contentLength;
    this.maxSkipDistance = maxSkipDistance;
  }

  /**
   * @return Length of the stream
   */
  public long getContentLength() {
    return contentLength;
  }

  /**
   * @return Current position in the stream
   */
  public abstract long getPosition();

  protected abstract void seekHard(long position) throws IOException;

  private void skipFully(long distance) throws IOException {
    long current = getPosition();
    long target = current + distance;

    while (current < target) {
      long skipped = skip(target - current);

      if (skipped == 0) {
        throw new IOException("Cannot skip any further.");
      }

      current += skipped;
    }
  }

  /**
   * Seek to the specified position
   * @param position The position to seek to
   * @throws IOException On a read error or if the position is beyond EOF
   */
  public void seek(long position) throws IOException {
    long current = getPosition();

    if (current <= position && position - current <= maxSkipDistance) {
      skipFully(position - current);
    } else {
      seekHard(position);
    }
  }
}
