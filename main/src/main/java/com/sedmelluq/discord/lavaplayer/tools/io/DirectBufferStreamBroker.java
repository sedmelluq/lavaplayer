package com.sedmelluq.discord.lavaplayer.tools.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * A helper class to consume the entire contents of a stream into a direct byte buffer. Designed for cases where this is
 * repeated several times, as it supports resetting.
 */
public class DirectBufferStreamBroker {
  private final byte[] copyBuffer;
  private final int initialSize;
  private ByteBuffer currentBuffer;

  /**
   * @param initialSize Initial size of the underlying direct buffer.
   */
  public DirectBufferStreamBroker(int initialSize) {
    this.initialSize = initialSize;
    this.copyBuffer = new byte[512];
    this.currentBuffer = ByteBuffer.allocateDirect(initialSize);
  }

  /**
   * Reset the buffer to its initial size.
   */
  public void resetAndCompact() {
    currentBuffer = ByteBuffer.allocateDirect(initialSize);
  }

  /**
   * Clear the underlying buffer.
   */
  public void clear() {
    currentBuffer.clear();
  }

  /**
   * @return A duplicate of the underlying buffer.
   */
  public ByteBuffer getBuffer() {
    ByteBuffer buffer = currentBuffer.duplicate();
    buffer.flip();
    return buffer;
  }

  /**
   * Consume an entire stream and append it into the buffer (or clear first if clear parameter is true).
   *
   * @param clear Whether to clear the buffer before appending the stream contents to it
   * @param inputStream The input stream to fully consume
   * @throws IOException On read error
   */
  public void consume(boolean clear, InputStream inputStream) throws IOException {
    if (clear) {
      clear();
    }

    ensureCapacity(currentBuffer.position() + inputStream.available());

    int chunk;

    while ((chunk = inputStream.read(copyBuffer)) != -1) {
      ensureCapacity(currentBuffer.position() + chunk);
      currentBuffer.put(copyBuffer, 0, chunk);
    }
  }

  private void ensureCapacity(int capacity) {
    if (capacity > currentBuffer.capacity()) {
      ByteBuffer newBuffer = ByteBuffer.allocateDirect(currentBuffer.capacity() << 1);
      currentBuffer.flip();

      newBuffer.put(currentBuffer);
      currentBuffer = newBuffer;
    }
  }
}
