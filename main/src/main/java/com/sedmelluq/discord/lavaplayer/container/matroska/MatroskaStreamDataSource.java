package com.sedmelluq.discord.lavaplayer.container.matroska;

import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import org.ebml.io.DataSource;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A seekable stream based implementation of JEBML DataSource interface
 */
public class MatroskaStreamDataSource implements DataSource {
  private final SeekableInputStream stream;
  private final long length;
  private byte[] tempBuffer;

  /**
   * @param stream The underlying seekable stream
   */
  public MatroskaStreamDataSource(SeekableInputStream stream) {
    this.stream = stream;
    this.length = stream.getContentLength();
  }

  @Override
  public byte readByte() {
    try {
      return (byte)stream.read();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private int readChunkFullyToBuffer(ByteBuffer buffer) {
    int actualReadLength;

    try {
      if (buffer.hasArray()) {
        int readSize = buffer.remaining();
        actualReadLength = stream.read(buffer.array(), buffer.arrayOffset() + (buffer.capacity() - readSize), readSize);
      } else {
        if (tempBuffer == null) {
          tempBuffer = new byte[1024];
        }

        int readSize = Math.min(buffer.remaining(), tempBuffer.length);
        actualReadLength = stream.read(tempBuffer, 0, readSize);
      }

      if (actualReadLength < 0) {
        throw new EOFException("Requested more than was available.");
      } else {
        buffer.position(buffer.position() + actualReadLength);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return actualReadLength;
  }

  @Override
  public int read(ByteBuffer buffer) {
    int totalReadLength = 0;

    while (buffer.remaining() > 0) {
      totalReadLength += readChunkFullyToBuffer(buffer);
    }

    return totalReadLength;
  }

  @Override
  public long skip(long offset) {
    try {
      long totalSkipped = 0;

      while (totalSkipped < offset) {
        long skipped = stream.skip(offset - totalSkipped);

        if (skipped > 0) {
          totalSkipped += skipped;
        } else {
          throw new EOFException("Tried to skip past the end.");
        }
      }

      return totalSkipped;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public long length() {
    return length;
  }

  @Override
  public long getFilePointer() {
    return stream.getPosition();
  }

  @Override
  public boolean isSeekable() {
    return true;
  }

  @Override
  public long seek(long pos) {
    try {
      stream.seek(pos);
      return pos;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
