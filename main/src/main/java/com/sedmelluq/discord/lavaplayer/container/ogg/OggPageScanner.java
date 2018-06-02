package com.sedmelluq.discord.lavaplayer.container.ogg;

import java.nio.ByteBuffer;

/**
 * Scanner for determining OGG stream information by seeking around in it.
 */
public class OggPageScanner {
  private static final int OGG_PAGE_HEADER_INT = ByteBuffer.wrap(new byte[] { 0x4F, 0x67, 0x67, 0x53 }).getInt(0);

  private final long absoluteOffset;
  private final byte[] data;
  private final int dataLength;

  private int flags;
  private long reversedPosition;
  private int pageSize;
  private long byteStreamPosition;

  /**
   * @param absoluteOffset Current position of the stream in bytes.
   * @param data Byte array with data starting at that position.
   * @param dataLength Length of data.
   */
  public OggPageScanner(long absoluteOffset, byte[] data, int dataLength) {
    this.absoluteOffset = absoluteOffset;
    this.data = data;
    this.dataLength = dataLength;
  }

  /**
   * @param firstPageOffset Absolute position of the first page in the stream.
   * @param sampleRate Sample rate of the track in the stream.
   * @return If the data contains the header of the last page in the OGG stream, then stream size information,
   *         otherwise <code>null</code>.
   */
  public OggStreamSizeInfo scanForSizeInfo(long firstPageOffset, int sampleRate) {
    ByteBuffer buffer = ByteBuffer.wrap(data, 0, dataLength);
    int head = buffer.getInt(0);

    for (int i = 0; i < dataLength - 27; i++) {
      if (head == OGG_PAGE_HEADER_INT) {
        buffer.position(i);

        if (attemptReadHeader(buffer)) {
          do {
            if ((flags & OggPageHeader.FLAG_LAST_PAGE) != 0) {
              return new OggStreamSizeInfo((byteStreamPosition - firstPageOffset) + pageSize,
                  Long.reverseBytes(reversedPosition), firstPageOffset, byteStreamPosition, sampleRate);
            }
          } while (attemptReadHeader(buffer));
        }
      }

      head <<= 8;
      head |= data[i + 4] & 0xFF;
    }

    return null;
  }

  private boolean attemptReadHeader(ByteBuffer buffer) {
    int start = buffer.position();

    if (buffer.limit() < start + 27) {
      return false;
    } else if (buffer.getInt(start) != OGG_PAGE_HEADER_INT) {
      return false;
    } else if (buffer.get(start + 4) != 0) {
      return false;
    }

    int segmentCount = buffer.get(start + 26) & 0xFF;
    int minimumCapacity = start + segmentCount + 27;

    if (buffer.limit() < minimumCapacity) {
      return false;
    }

    int segmentBase = start + 27;

    for (int i = 0; i < segmentCount; i++) {
      minimumCapacity += buffer.get(segmentBase + i) & 0xFF;
    }

    if (buffer.limit() < minimumCapacity) {
      return false;
    }

    flags = buffer.get(start + 5) & 0xFF;
    reversedPosition = buffer.getLong(start + 6);
    byteStreamPosition = absoluteOffset + start;
    pageSize = minimumCapacity;

    buffer.position(minimumCapacity);
    return true;
  }
}
