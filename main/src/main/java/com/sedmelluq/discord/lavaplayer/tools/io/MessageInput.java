package com.sedmelluq.discord.lavaplayer.tools.io;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.input.CountingInputStream;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An input for messages with their size known so unknown messages can be skipped.
 */
public class MessageInput {
  private final CountingInputStream countingInputStream;
  private final DataInputStream dataInputStream;
  private int messageSize;
  private int messageFlags;

  /**
   * @param inputStream Input stream to read from.
   */
  public MessageInput(InputStream inputStream) {
    this.countingInputStream = new CountingInputStream(inputStream);
    this.dataInputStream = new DataInputStream(inputStream);
  }

  /**
   * @return Data input for the next message. Note that it does not automatically skip over the last message if it was
   *         not fully read, for that purpose, skipRemainingBytes() should be explicitly called after reading every
   *         message. A null return value indicates the position where MessageOutput#finish() had written the end
   *         marker.
   * @throws IOException On IO error
   */
  public DataInput nextMessage() throws IOException {
    int value = dataInputStream.readInt();
    messageFlags = (int) ((value & 0xC0000000L) >> 30L);
    messageSize = value & 0x3FFFFFFF;

    if (messageSize == 0) {
      return null;
    }

    return new DataInputStream(new BoundedInputStream(countingInputStream, messageSize));
  }

  /**
   * @return Flags (values 0-3) of the last message for which nextMessage() was called.
   */
  public int getMessageFlags() {
    return messageFlags;
  }

  /**
   * Skip the remaining bytes of the last message returned from nextMessage(). This must be called if it is not certain
   * that all of the bytes of the message were consumed.
   * @throws IOException On IO error
   */
  public void skipRemainingBytes() throws IOException {
    long count = countingInputStream.resetByteCount();

    if (count < messageSize) {
      IOUtils.skipFully(dataInputStream, messageSize - count);
    }

    messageSize = 0;
  }
}
