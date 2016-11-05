package com.sedmelluq.discord.lavaplayer.container.mpegts;

import com.sedmelluq.discord.lavaplayer.tools.io.GreedyInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Input stream which takes in a stream providing PES-wrapped media packets and outputs provides the raw content of it
 * upstream.
 */
public class PesPacketInputStream extends InputStream {
  private static final byte[] SYNC_BYTES = new byte[] { 0x00, 0x00, 0x01 };

  private final InputStream inputStream;
  private final byte[] lengthBufferBytes;
  private final ByteBuffer lengthBuffer;
  private int packetBytesLeft;

  /**
   * @param inputStream Underlying input stream.
   */
  public PesPacketInputStream(InputStream inputStream) {
    this.inputStream = new GreedyInputStream(inputStream);
    this.lengthBufferBytes = new byte[2];
    this.lengthBuffer = ByteBuffer.wrap(lengthBufferBytes);
  }

  private boolean makeBytesAvailable() throws IOException {
    if (packetBytesLeft > 0) {
      return true;
    }

    int streamByte;
    int matched = 0;
    boolean packetFound = false;

    while (!packetFound && (streamByte = inputStream.read()) != -1) {
      if (streamByte == SYNC_BYTES[matched]) {
        if (++matched == SYNC_BYTES.length) {
          matched = 0;
          packetFound = processPacketHeader();
        }
      } else {
        matched = 0;
      }
    }

    return packetFound;
  }

  private boolean processPacketHeader() throws IOException {
    // No need to check stream ID value
    if (inputStream.read() == -1 || inputStream.read(lengthBufferBytes) != lengthBufferBytes.length) {
      return false;
    }

    int length = lengthBuffer.getShort(0);
    if (inputStream.skip(2) != 2) {
      return false;
    }

    int headerLength = inputStream.read();
    if (headerLength == -1 || inputStream.skip(headerLength) != headerLength) {
      return false;
    }

    packetBytesLeft = length - 3 - headerLength;
    return packetBytesLeft > 0;
  }

  @Override
  public int read() throws IOException {
    if (!makeBytesAvailable()) {
      return -1;
    }

    int result = inputStream.read();
    if (result >= 0) {
      packetBytesLeft--;
    }

    return result;
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    if (!makeBytesAvailable()) {
      return -1;
    }

    int chunk = Math.min(packetBytesLeft, length);
    int result = inputStream.read(buffer, offset, chunk);
    if (result > 0) {
      packetBytesLeft -= result;
    }

    return result;
  }

  @Override
  public int available() throws IOException {
    return packetBytesLeft;
  }
}
