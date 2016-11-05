package com.sedmelluq.discord.lavaplayer.container.mpegts;

import com.sedmelluq.discord.lavaplayer.tools.io.BitBufferReader;
import com.sedmelluq.discord.lavaplayer.tools.io.GreedyInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Input stream which takes in a stream providing MPEG TS data and outputs a single track from it specified by the
 * elementary data type.
 */
public class MpegTsElementaryInputStream extends InputStream {
  public static final int ADTS_ELEMENTARY_STREAM = 0x0F;

  private static final int PID_UNKNOWN = -1;
  private static final int PID_NOT_PRESENT = -2;

  private static final int TS_PACKET_SIZE = 188;

  private final InputStream inputStream;
  private final int elementaryDataType;
  private final byte[] packet;
  private final ByteBuffer packetBuffer;
  private final BitBufferReader bufferReader;
  private int elementaryStreamIdentifier;
  private int programMapIdentifier;
  private boolean elementaryDataInPacket;
  private boolean streamEndReached;

  /**
   * @param inputStream Underlying input stream
   * @param elementaryDataType ID of the media type to pass upstream
   */
  public MpegTsElementaryInputStream(InputStream inputStream, int elementaryDataType) {
    this.inputStream = new GreedyInputStream(inputStream);
    this.elementaryDataType = elementaryDataType;
    this.packet = new byte[TS_PACKET_SIZE];
    this.packetBuffer = ByteBuffer.wrap(packet);
    this.bufferReader = new BitBufferReader(packetBuffer);
    this.elementaryStreamIdentifier = PID_UNKNOWN;
    this.programMapIdentifier = PID_UNKNOWN;
  }

  @Override
  public int read() throws IOException {
    if (!findElementaryData()) {
      return -1;
    }

    int result = packetBuffer.get() & 0xFF;

    checkElementaryDataEnd();
    return result;
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    if (!findElementaryData()) {
      return -1;
    }

    int chunk = Math.min(length, packetBuffer.remaining());
    packetBuffer.get(buffer, offset, chunk);

    checkElementaryDataEnd();

    return chunk;
  }

  private boolean findElementaryData() throws IOException {
    if (!elementaryDataInPacket) {
      while (processPacket()) {
        if (elementaryDataInPacket) {
          return true;
        }
      }
    }

    return elementaryDataInPacket;
  }

  private void checkElementaryDataEnd() {
    if (packetBuffer.remaining() == 0) {
      elementaryDataInPacket = false;
    }
  }

  private boolean processPacket() throws IOException {
    if (!isContinuable()) {
      return false;
    } else if (inputStream.read(packet) < packet.length) {
      streamEndReached = true;
      return false;
    }

    packetBuffer.clear();
    bufferReader.readRemainingBits();

    int identifier = verifyPacket(bufferReader, packetBuffer);
    if (identifier == -1) {
      return false;
    }

    processPacketContent(identifier);
    return isContinuable();
  }

  private void processPacketContent(int identifier) {
    if (identifier == 0 || identifier == programMapIdentifier) {
      if (identifier == 0) {
        programMapIdentifier = PID_NOT_PRESENT;
      }

      processProgramPacket();
    } else if (identifier == elementaryStreamIdentifier) {
      elementaryDataInPacket = true;
    }
  }

  private boolean isContinuable() {
    return !streamEndReached || programMapIdentifier != PID_NOT_PRESENT && elementaryStreamIdentifier != PID_NOT_PRESENT;
  }

  private void processProgramPacket() {
    discardPointerField();

    while (packetBuffer.hasRemaining()) {
      int tableIdentifier = packetBuffer.get() & 0xFF;
      if (tableIdentifier == 0xFF) {
        break;
      }

      int sectionInfo = bufferReader.asInteger(6);
      int sectionLength = bufferReader.asInteger(10);
      int position = packetBuffer.position();
      bufferReader.readRemainingBits();

      if (tableIdentifier == 0) {
        processPatTable(sectionInfo);
      } else if (tableIdentifier == 2) {
        processPmtTable(sectionInfo, sectionLength);
      }

      packetBuffer.position(position + sectionLength);
    }
  }

  private boolean processPatPmtCommon(int sectionInfo) {
    if (sectionInfo != 0x2C) {
      return false;
    }

    // Table syntax section, boring.
    bufferReader.asLong(40);
    return true;
  }

  private void processPatTable(int sectionInfo) {
    if (!processPatPmtCommon(sectionInfo)) {
      return;
    }

    // Program number
    bufferReader.asLong(16);

    if (bufferReader.asLong(3) == 0x07) {
      programMapIdentifier = bufferReader.asInteger(13);
    }
  }

  private void processPmtTable(int sectionInfo, int sectionLength) {
    int endPosition = packetBuffer.position() + sectionLength;

    if (!processPatPmtCommon(sectionInfo) || bufferReader.asInteger(3) != 0x07) {
      return;
    }

    // Clock packet identifier (PCR PID)
    bufferReader.asLong(13);
    // Reserved bits (must be 1111) and program info length unused bits (must be 00)
    if (bufferReader.asLong(6) != 0x3C) {
      return;
    }

    // Skip program descriptors
    int programInfoLength = bufferReader.asInteger(10);
    packetBuffer.position(packetBuffer.position() + programInfoLength);

    processElementaryStreams(endPosition);
  }

  private void processElementaryStreams(int endPosition) {
    elementaryStreamIdentifier = PID_NOT_PRESENT;

    while (packetBuffer.position() < endPosition - 4) {
      int streamType = bufferReader.asInteger(8);
      // Reserved bits (must be 111)
      bufferReader.asInteger(3);

      int streamPid = bufferReader.asInteger(13);
      // 4 reserved bits (1111) and 2 ES Info length unused bits (00)
      bufferReader.asLong(6);

      int infoLength = bufferReader.asInteger(10);
      packetBuffer.position(packetBuffer.position() + infoLength);

      if (streamType == elementaryDataType) {
        elementaryStreamIdentifier = streamPid;
      }
    }
  }

  private void discardPointerField() {
    int pointerField = packetBuffer.get();

    for (int i = 0; i < pointerField; i++) {
      packetBuffer.get();
    }
  }

  private static int verifyPacket(BitBufferReader reader, ByteBuffer rawBuffer) {
    if (reader.asLong(8) != 'G') {
      return -1;
    }

    // Not important for this case
    reader.asLong(3);

    int identifier = reader.asInteger(13);
    long scrambling = reader.asLong(2);

    // Adaptation
    long adaptation = reader.asLong(2);

    if (scrambling != 0) {
      return -1;
    }

    // Continuity counter
    reader.asLong(4);

    if (adaptation == 2 || adaptation == 3) {
      int adaptationSize = reader.asInteger(8);
      rawBuffer.position(rawBuffer.position() + adaptationSize);
    }

    return identifier;
  }
}
