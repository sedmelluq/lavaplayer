package com.sedmelluq.discord.lavaplayer.remote.message;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Handles encoding and decoding of messages.
 */
public class RemoteMessageMapper {
  private final Map<Class<? extends RemoteMessage>, RemoteMessageType> encodingMap;

  /**
   * Create a new instance.
   */
  public RemoteMessageMapper() {
    encodingMap = new IdentityHashMap<>();

    initialiseEncodingMap();
  }

  private void initialiseEncodingMap() {
    for (RemoteMessageType type : RemoteMessageType.class.getEnumConstants()) {
      encodingMap.put(type.codec.getMessageClass(), type);
    }
  }

  /**
   * Decodes one message. If the input stream indicates the end of messages, null is returned.
   *
   * @param input The input stream containing the message
   * @return The decoded message or null in case no more messages are present
   * @throws IOException When an IO error occurs
   */
  public RemoteMessage decode(DataInput input) throws IOException {
    int messageSize = input.readInt();
    if (messageSize == 0) {
      return null;
    }

    RemoteMessageType type = RemoteMessageType.class.getEnumConstants()[input.readByte() & 0xFF];
    return type.codec.decode(input);
  }

  /**
   * Encodes one message.
   *
   * @param output The output stream to encode to
   * @param message The message to encode
   * @throws IOException When an IO error occurs
   */
  @SuppressWarnings("unchecked")
  public void encode(DataOutputStream output, RemoteMessage message) throws IOException {
    RemoteMessageType type = encodingMap.get(message.getClass());

    ByteArrayOutputStream messageOutputBytes = new ByteArrayOutputStream();
    DataOutput messageOutput = new DataOutputStream(messageOutputBytes);

    RemoteMessageCodec codec = type.codec;
    codec.encode(messageOutput, message);

    output.writeInt(messageOutputBytes.size() + 1);
    output.writeByte((byte) type.ordinal());
    messageOutputBytes.writeTo(output);
  }

  /**
   * Write the marker to indicate no more messages are in the stream.
   *
   * @param output The output stream
   * @throws IOException When an IO error occurs
   */
  public void endOutput(DataOutputStream output) throws IOException {
    output.writeInt(0);
  }
}
