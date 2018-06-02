package com.sedmelluq.discord.lavaplayer.tools.io;

import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoProvider;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * An input stream that is seekable.
 */
public abstract class SeekableInputStream extends InputStream {
  protected long contentLength;
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
   * @return Maximum distance that this stream will skip without doing a direct seek on the underlying resource.
   */
  public long getMaxSkipDistance() {
    return maxSkipDistance;
  }

  /**
   * @return Current position in the stream
   */
  public abstract long getPosition();

  protected abstract void seekHard(long position) throws IOException;

  /**
   * @return <code>true</code> if it is possible to seek to an arbitrary position in this stream, even when it is behind
   *         the current position.
   */
  public abstract boolean canSeekHard();

  /**
   * Skip the specified number of bytes in the stream. The result is either that the requested number of bytes were
   * skipped or an EOFException was thrown.
   * @param distance The number of bytes to skip
   * @throws IOException On IO error
   */
  public void skipFully(long distance) throws IOException {
    long current = getPosition();
    long target = current + distance;

    while (current < target) {
      long skipped = skip(target - current);

      if (skipped == 0) {
        if (read() == -1) {
          throw new EOFException("Cannot skip any further.");
        } else {
          skipped = 1;
        }
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

    if (current != position) {
      if (current <= position && position - current <= maxSkipDistance) {
        skipFully(position - current);
      } else if (!canSeekHard()) {
        if (current > position) {
          seekHard(0);
          skipFully(position);
        } else {
          skipFully(position - current);
        }
      } else {
        seekHard(position);
      }
    }
  }

  public abstract List<AudioTrackInfoProvider> getTrackInfoProviders();
}
