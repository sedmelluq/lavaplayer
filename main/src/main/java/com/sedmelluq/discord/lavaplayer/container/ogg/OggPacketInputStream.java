package com.sedmelluq.discord.lavaplayer.container.ogg;

import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.StreamTools;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.checkNextBytes;

/**
 * This provides a stream for OGG packets where the stream is always bounded to the current packet, and the next packet
 * can be started with startNewPacket(). The same way it is bound to a specific track and the next track can be started
 * with startNewTrack() when the previous one has ended (startNewPacket() has returned false).
 */
public class OggPacketInputStream extends InputStream {
  static final int[] OGG_PAGE_HEADER = new int[] { 0x4F, 0x67, 0x67, 0x53 };

  private static final int SHORT_SCAN = 10240;
  private static final int LONG_SCAN = 65307;

  private final SeekableInputStream inputStream;
  private final DataInput dataInput;
  private final int[] segmentSizes;

  private OggPageHeader pageHeader;
  private int bytesLeftInPacket;
  private boolean packetContinues;
  private int nextPacketSegmentIndex;
  private State state;

  /**
   * @param inputStream Input stream to read in as OGG packets
   */
  public OggPacketInputStream(SeekableInputStream inputStream) {
    this.inputStream = inputStream;
    this.dataInput = new DataInputStream(inputStream);
    this.segmentSizes = new int[256];
    this.state = State.TRACK_BOUNDARY;
  }

  /**
   * Load the next track from the stream. This is only valid when the stream is in a track boundary state.
   * @return True if next track is present in the stream, false if the stream has terminated.
   */
  public boolean startNewTrack() {
    if (state == State.TERMINATED) {
      return false;
    } else if (state != State.TRACK_BOUNDARY) {
      throw new IllegalStateException("Cannot load the next track while the previous one has not been consumed.");
    }

    pageHeader = null;
    state = State.PACKET_BOUNDARY;
    return true;
  }

  /**
   * Load the next packet from the stream. This is only valid when the stream is in a packet boundary state.
   * @return True if next packet is present in the track. State is PACKET_READ.
   *         False if the track is finished. State is either TRACK_BOUNDARY or TERMINATED.
   * @throws IOException On read error.
   */
  public boolean startNewPacket() throws IOException {
    if (state == State.TRACK_BOUNDARY) {
      return false;
    } else if (state != State.PACKET_BOUNDARY) {
      throw new IllegalStateException("Cannot start a new packet while the previous one has not been consumed.");
    }

    if ((pageHeader == null || nextPacketSegmentIndex == pageHeader.segmentCount) && !loadNextNonEmptyPage()) {
      return false;
    }

    initialisePacket();
    return true;
  }

  private boolean readPageHeader() throws IOException {
    if (!checkNextBytes(inputStream, OGG_PAGE_HEADER, false)) {
      if (inputStream.read() == -1) {
        return false;
      }

      throw new IllegalStateException("Stream is not positioned at a page header.");
    } else if ((dataInput.readByte() & 0xFF) != 0) {
      throw new IllegalStateException("Unknown OGG stream version.");
    }

    int flags = dataInput.readByte() & 0xFF;
    long position = Long.reverseBytes(dataInput.readLong());
    int streamIdentifier = Integer.reverseBytes(dataInput.readInt());
    int pageSequence = Integer.reverseBytes(dataInput.readInt());
    int checksum = Integer.reverseBytes(dataInput.readInt());
    int segmentCount = dataInput.readByte() & 0xFF;
    long byteStreamPosition = inputStream.getPosition() - 27;

    pageHeader = new OggPageHeader(flags, position, streamIdentifier, pageSequence, checksum, segmentCount,
        byteStreamPosition);

    for (int i = 0; i < segmentCount; i++) {
      segmentSizes[i] = dataInput.readByte() & 0xFF;
    }

    return true;
  }

  /**
   * Load pages until a non-empty page is reached. Valid to call in states PACKET_BOUNDARY (page starts a new packet) or
   * PACKET_READ (page starts with a continuation).
   *
   * @return True if a page belonging to the same track was loaded, state is PACKET_READ.
   *         False if the next page cannot be loaded because the current one ended the track, state is TRACK_BOUNDARY
   *         or TERMINATED.
   * @throws IOException On read error.
   */
  private boolean loadNextNonEmptyPage() throws IOException {
    do {
      if (!loadNextPage()) {
        return false;
      }
    } while (pageHeader.segmentCount == 0);

    return true;
  }

  /**
   * Load the next page from the stream. Valid to call in states PACKET_BOUNDARY (page starts a new packet) or
   * PACKET_READ (page starts with a continuation).
   *
   * @return True if a page belonging to the same track was loaded, state is PACKET_READ.
   *         False if the next page cannot be loaded because the current one ended the track, state is TRACK_BOUNDARY
   *         or TERMINATED.
   * @throws IOException On read error.
   */
  private boolean loadNextPage() throws IOException {
    if (pageHeader != null && pageHeader.isLastPage) {
      if (packetContinues) {
        throw new IllegalStateException("Track finished in the middle of a packet.");
      }

      state = State.TRACK_BOUNDARY;
      return false;
    }

    if (!readPageHeader()) {
      if (packetContinues) {
        throw new IllegalStateException("Stream ended in the middle of a packet.");
      }
      return false;
    }

    nextPacketSegmentIndex = 0;
    state = State.PACKET_READ;
    return true;
  }

  /**
   * Initialise the (remainder of the) current packet in the stream. This may be called either to initialise a new
   * packet or a continuation of the previous one. Call only in state PACKET_READ.
   *
   * @return Returns false if the remaining size of the packet was zero, state is PACKET_BOUNDARY.
   *         Returns true if the initialised packet has any bytes in it, state is PACKET_READ.
   */
  private boolean initialisePacket() {
    while (nextPacketSegmentIndex < pageHeader.segmentCount) {
      int size = segmentSizes[nextPacketSegmentIndex++];
      bytesLeftInPacket += size;

      if (size < 255) {
        // Anything below 255 is also a packet end marker.
        if (bytesLeftInPacket == 0) {
          // We reached packet end without getting any additional bytes, set state to packet boundary
          state = State.PACKET_BOUNDARY;
          return false;
        }

        // We reached packet end and got some more bytes.
        packetContinues = false;
        return true;
      }
    }

    // Packet does not end within this page.
    packetContinues = true;
    return true;
  }

  @Override
  public int read() throws IOException {
    if (bytesLeftInPacket == 0) {
      return -1;
    }

    int value = inputStream.read();
    if (value == -1) {
      return -1;
    }

    if (--bytesLeftInPacket == 0) {
      continuePacket();
    }

    return value;
  }

  @Override
  public int read(byte[] buffer, int initialOffset, int length) throws IOException {
    int currentOffset = initialOffset;
    int maximumOffset = initialOffset + length;

    // Terminates when we have read as much as we needed
    while (currentOffset < maximumOffset) {
      // If there is nothing left in the current packet, stream is in EOF state
      if (bytesLeftInPacket == 0) {
        return -1;
      }

      // Limit the read size to the number of bytes that are definitely still left in the packet
      int chunk = Math.min(maximumOffset - currentOffset, bytesLeftInPacket);
      int read = inputStream.read(buffer, currentOffset, chunk);

      if (read == -1) {
        // EOF in the underlying stream before the end of a packet. Throw an exception, the consumer should not need
        // to check for partial packets.
        throw new EOFException("Underlying stream ended before the end of a packet.");
      }

      currentOffset += read;
      bytesLeftInPacket -= read;

      if (bytesLeftInPacket == 0) {
        // We got everything from our chunk of size min(leftInPacket, requested) and also exhausted the bytes that we
        // know the packet had left. Check if the packet continues so we could continue fetching from the same packet.
        // Otherwise, bugger out.

        if (!continuePacket()) {
          break;
        }
      } else if (read < chunk) {
        // The underlying stream cannot provide more right now. Let it rest.
        return currentOffset - initialOffset;
      }
    }

    return currentOffset - initialOffset;
  }

  @Override
  public int available() throws IOException {
    if (state != State.PACKET_READ) {
      return 0;
    }

    return Math.min(inputStream.available(), bytesLeftInPacket);
  }

  /**
   * If it is possible to seek backwards on this stream, and the length of the stream is known, seeks to the end of the
   * track to determine the stream length both in bytes and samples.
   *
   * @param sampleRate Sample rate of the track in this stream.
   * @return OGG stream size information.
   * @throws IOException On read error.
   */
  public OggStreamSizeInfo seekForSizeInfo(int sampleRate) throws IOException {
    if (!inputStream.canSeekHard()) {
      return null;
    }

    long savedPosition = inputStream.getPosition();

    OggStreamSizeInfo sizeInfo = scanForSizeInfo(SHORT_SCAN, sampleRate);

    if (sizeInfo == null) {
      sizeInfo = scanForSizeInfo(LONG_SCAN, sampleRate);
    }

    inputStream.seek(savedPosition);
    return sizeInfo;
  }

  private OggStreamSizeInfo scanForSizeInfo(int tailLength, int sampleRate) throws IOException {
    if (pageHeader == null) {
      return null;
    }

    long absoluteOffset = Math.max(pageHeader.byteStreamPosition, inputStream.getContentLength() - tailLength);
    inputStream.seek(absoluteOffset);

    byte[] data = new byte[tailLength];
    int dataLength = StreamTools.readUntilEnd(inputStream, data, 0, data.length);

    return new OggPageScanner(absoluteOffset, data, dataLength).scanForSizeInfo(pageHeader.byteStreamPosition,
        sampleRate);
  }

  /**
   * Process request for more bytes for the packet. Call only when the state is PACKET_READ.
   *
   * @return Returns false if no more bytes for the packet are available, state is PACKET_BOUNDARY.
   *         Returns true if more bytes were fetched for this packet, state is PACKET_READ.
   * @throws IOException On read error.
   */
  private boolean continuePacket() throws IOException {
    if (!packetContinues) {
      // We have reached the end of the packet.
      state = State.PACKET_BOUNDARY;
      return false;
    }

    // Load more segments for this packet from the next page.
    if (!loadNextNonEmptyPage()) {
      throw new IllegalStateException("Track or stream end reached within an incomplete packet.");
    } else if (!initialisePacket()) {
      return false;
    }

    return true;
  }

  private enum State {
    TRACK_BOUNDARY,
    PACKET_BOUNDARY,
    PACKET_READ,
    TERMINATED
  }
}
