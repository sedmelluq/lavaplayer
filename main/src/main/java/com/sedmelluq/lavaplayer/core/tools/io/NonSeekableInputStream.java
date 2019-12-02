package com.sedmelluq.lavaplayer.core.tools.io;

import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoBuilder;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.input.CountingInputStream;

public class NonSeekableInputStream extends SeekableInputStream {
  private final CountingInputStream delegate;

  public NonSeekableInputStream(InputStream delegate) {
    super(Long.MAX_VALUE, 0);
    this.delegate = new CountingInputStream(delegate);
  }

  @Override
  public long getPosition() {
    return delegate.getByteCount();
  }

  @Override
  protected void seekHard(long position) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean canSeekHard() {
    return false;
  }

  @Override
  public int read() throws IOException {
    return delegate.read();
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    return delegate.read(buffer, offset, length);
  }

  @Override
  public void provideTrackInfo(AudioTrackInfoBuilder builder) {

  }
}
