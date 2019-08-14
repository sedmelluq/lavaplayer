package com.sedmelluq.discord.lavaplayer.container.adts;

import com.sedmelluq.discord.lavaplayer.tools.io.BitBufferReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Finds and reads ADTS packet headers from an input stream.
 */
public class AdtsStreamReader {
  private static final AdtsPacketHeader EOF_PACKET = new AdtsPacketHeader(false, 0, 0, 0, 0);

  private static final int HEADER_BASE_SIZE = 7;
  private static final int INVALID_VALUE = -1;

  private static final int[] sampleRateMapping = new int[] {
      96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050,
      16000, 12000, 11025, 8000, 7350, INVALID_VALUE, INVALID_VALUE, INVALID_VALUE
  };

  private final InputStream inputStream;
  private final byte[] scanBuffer;
  private final ByteBuffer scanByteBuffer;
  private final BitBufferReader scanBufferReader;
  private AdtsPacketHeader currentPacket;

  /**
   * @param inputStream The input stream to use.
   */
  public AdtsStreamReader(InputStream inputStream) {
    this.inputStream = inputStream;
    this.scanBuffer = new byte[32];
    this.scanByteBuffer = ByteBuffer.wrap(scanBuffer);
    this.scanBufferReader = new BitBufferReader(scanByteBuffer);
  }

  /**
   * Scan the input stream for an ADTS packet header. Subsequent calls will return the same header until nextPacket() is
   * called.
   *
   * @return The packet header if found before EOF.
   * @throws IOException On read error.
   */
  public AdtsPacketHeader findPacketHeader() throws IOException {
    return findPacketHeader(Integer.MAX_VALUE);
  }

  /**
   * Scan the input stream for an ADTS packet header. Subsequent calls will return the same header until nextPacket() is
   * called.
   *
   * @param maximumDistance Maximum number of bytes to scan.
   * @return The packet header if found before EOF and maximum byte limit.
   * @throws IOException On read error.
   */
  public AdtsPacketHeader findPacketHeader(int maximumDistance) throws IOException {
    if (currentPacket == null) {
      currentPacket = scanForPacketHeader(maximumDistance);
    }

    return currentPacket == EOF_PACKET ? null : currentPacket;
  }

  /**
   * Resets the current packet, makes next calls to findPacketHeader scan for the next occurrence in the stream.
   */
  public void nextPacket() {
    currentPacket = null;
  }

  private AdtsPacketHeader scanForPacketHeader(int maximumDistance) throws IOException {
    int bufferPosition = 0;

    for (int i = 0; i < maximumDistance; i++) {
      int nextByte = inputStream.read();

      if (nextByte == -1) {
        return EOF_PACKET;
      }

      scanBuffer[bufferPosition++] = (byte) nextByte;

      if (bufferPosition >= HEADER_BASE_SIZE) {
        AdtsPacketHeader header = readHeaderFromBufferTail(bufferPosition);

        if (header != null) {
          return header;
        }
      }

      if (bufferPosition == scanBuffer.length) {
        copyEndToBeginning(scanBuffer, HEADER_BASE_SIZE);
        bufferPosition = HEADER_BASE_SIZE;
      }
    }

    return null;
  }

  private AdtsPacketHeader readHeaderFromBufferTail(int position) throws IOException {
    scanByteBuffer.position(position - HEADER_BASE_SIZE);

    AdtsPacketHeader header = readHeader(scanBufferReader);
    scanBufferReader.readRemainingBits();

    if (header == null) {
      return null;
    } else if (!header.isProtectionAbsent) {
      int crcFirst = inputStream.read();
      int crcSecond = inputStream.read();

      if (crcFirst == -1 || crcSecond == -1) {
        return EOF_PACKET;
      }
    }

    return header;
  }

  private static void copyEndToBeginning(byte[] buffer, int chunk) {
    for (int i = 0; i < chunk; i++) {
      buffer[i] = buffer[buffer.length - chunk + i];
    }
  }

  private static AdtsPacketHeader readHeader(BitBufferReader reader) {
    if ((reader.asLong(15) & 0x7FFB) != 0x7FF8) {
      // Possible reasons:
      // 1) Syncword is not present, cannot be an ADTS header
      // 2) Layer value is not 0, which must always be 0 for ADTS
      return null;
    }

    boolean isProtectionAbsent = reader.asLong(1) == 1;
    int profile = reader.asInteger(2);
    int sampleRate = sampleRateMapping[reader.asInteger(4)];

    // Private bit
    reader.asLong(1);

    int channels = reader.asInteger(3);

    if (sampleRate == INVALID_VALUE || channels == 0) {
      return null;
    }

    // 4 boring bits
    reader.asLong(4);

    int frameLength = reader.asInteger(13);
    int payloadLength = frameLength - 7 - (isProtectionAbsent ? 0 : 2);

    // More boring bits
    reader.asLong(11);

    if (reader.asLong(2) != 0) {
      // Not handling multiple frames per packet
      return null;
    }

    return new AdtsPacketHeader(isProtectionAbsent, profile + 1, sampleRate, channels, payloadLength);
  }
}
