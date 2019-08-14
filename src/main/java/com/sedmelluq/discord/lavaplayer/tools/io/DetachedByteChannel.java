package com.sedmelluq.discord.lavaplayer.tools.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;

/**
 * Creates a readable byte channel which can be closed without closing the underlying channel.
 */
public class DetachedByteChannel implements ReadableByteChannel {
  private final ReadableByteChannel delegate;
  private boolean closed;

  /**
   * @param delegate The underlying channel
   */
  public DetachedByteChannel(ReadableByteChannel delegate) {
    this.delegate = delegate;
  }

  @Override
  public int read(ByteBuffer output) throws IOException {
    if (closed) {
      throw new ClosedChannelException();
    }

    return delegate.read(output);
  }

  @Override
  public boolean isOpen() {
    return !closed && delegate.isOpen();
  }

  @Override
  public void close() throws IOException {
    closed = true;
  }
}
