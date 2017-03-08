package com.sedmelluq.discord.lavaplayer.tools.io;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An output for  a series of messages which each have sizes specified before the start of the message. Even when the
 * decoder does not recognize some of the messages, it can skip over the message since it knows its size in advance.
 */
public class MessageOutput {
  private final OutputStream outputStream;
  private final DataOutputStream dataOutputStream;
  private final ByteArrayOutputStream messageByteOutput;
  private final DataOutputStream messageDataOutput;

  /**
   * @param outputStream Output stream to write the messages to
   */
  public MessageOutput(OutputStream outputStream) {
    this.outputStream = outputStream;
    this.dataOutputStream = new DataOutputStream(outputStream);
    this.messageByteOutput = new ByteArrayOutputStream();
    this.messageDataOutput = new DataOutputStream(messageByteOutput);
  }

  /**
   * @return Data output for a new message
   */
  public DataOutput startMessage() {
    messageByteOutput.reset();
    return messageDataOutput;
  }

  /**
   * Commit previously started message to the underlying output stream.
   * @throws IOException On IO error
   */
  public void commitMessage() throws IOException {
    dataOutputStream.writeInt(messageByteOutput.size());
    messageByteOutput.writeTo(outputStream);
  }

  /**
   * Commit previously started message to the underlying output stream.
   * @param flags Flags to use when committing the message (0-3).
   * @throws IOException On IO error
   */
  public void commitMessage(int flags) throws IOException {
    dataOutputStream.writeInt(messageByteOutput.size() | flags << 30);
    messageByteOutput.writeTo(outputStream);
  }

  /**
   * Write an end marker to the stream so that decoder knows to return null at this position.
   * @throws IOException On IO error
   */
  public void finish() throws IOException {
    dataOutputStream.writeInt(0);
  }
}
