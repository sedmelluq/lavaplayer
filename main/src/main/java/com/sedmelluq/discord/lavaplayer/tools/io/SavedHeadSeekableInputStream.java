package com.sedmelluq.discord.lavaplayer.tools.io;

import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoProvider;

import java.io.IOException;
import java.util.List;

/**
 * A wrapper around a seekable input stream which saves the beginning of the stream into a buffer. Seeking within the
 * saved beginning does not cause any IO to be done on the underlying input stream.
 */
public class SavedHeadSeekableInputStream extends SeekableInputStream {
  private final SeekableInputStream delegate;
  private final byte[] savedHead;
  private boolean usingHead;
  private long headPosition;
  private long savedUntilPosition;

  /**
   * @param delegate The seekable stream to delegate reading to
   * @param savedSize Number of bytes to buffer
   */
  public SavedHeadSeekableInputStream(SeekableInputStream delegate, int savedSize) {
    super(delegate.getContentLength(), delegate.getMaxSkipDistance());
    this.delegate = delegate;
    this.savedHead = new byte[savedSize];
  }

  /**
   * Load the number of bytes specified in the constructor into the saved buffer.
   * @throws IOException On IO error
   */
  public void loadHead() throws IOException {
    delegate.seek(0);
    savedUntilPosition = read(savedHead, 0, savedHead.length);
    usingHead = savedUntilPosition > 0;
    headPosition = 0;
  }

  @Override
  public long getPosition() {
    if (usingHead) {
      return headPosition;
    } else {
      return delegate.getPosition();
    }
  }

  @Override
  protected void seekHard(long position) throws IOException {
    if (position >= savedUntilPosition) {
      usingHead = false;
      delegate.seekHard(position);
    } else {
      usingHead = true;
      headPosition = position;
    }
  }

  @Override
  public boolean canSeekHard() {
    return delegate.canSeekHard();
  }

  @Override
  public List<AudioTrackInfoProvider> getTrackInfoProviders() {
    return delegate.getTrackInfoProviders();
  }

  @Override
  public int read() throws IOException {
    if (usingHead) {
      byte result = savedHead[(int) headPosition];

      if (++headPosition == savedUntilPosition) {
        delegate.seek(savedUntilPosition);
        usingHead = false;
      }

      return result & 0xFF;
    } else {
      return delegate.read();
    }
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (usingHead) {
      return super.read(b, off, len);
    } else {
      return delegate.read(b, off, len);
    }
  }

  @Override
  public long skip(long n) throws IOException {
    if (usingHead) {
      return super.skip(n);
    } else {
      return delegate.skip(n);
    }
  }

  @Override
  public int available() throws IOException {
    if (usingHead) {
      return (int) (savedUntilPosition - headPosition);
    } else {
      return delegate.available();
    }
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }

  @Override
  public boolean markSupported() {
    return false;
  }
}
