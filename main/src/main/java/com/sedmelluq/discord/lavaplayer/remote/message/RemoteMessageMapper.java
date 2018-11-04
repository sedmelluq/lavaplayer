package com.sedmelluq.discord.lavaplayer.remote.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private static final Logger log = LoggerFactory.getLogger(RemoteMessageMapper.class);

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

    RemoteMessageType[] types = RemoteMessageType.class.getEnumConstants();
    int typeIndex = input.readByte() & 0xFF;
    int version = input.readByte() & 0xFF;

    if (typeIndex >= types.length) {
      log.warn("Invalid message type {}.", typeIndex);
      input.readFully(new byte[messageSize - 1]);
      return UnknownMessage.INSTANCE;
    }

    RemoteMessageType type = types[typeIndex];

    if (version < 1 || version > type.codec.version(null)) {
      log.warn("Invalid version {} for message {}.", version, type);
      input.readFully(new byte[messageSize - 2]);
      return UnknownMessage.INSTANCE;
    }

    return type.codec.decode(input, version);
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

    output.writeInt(messageOutputBytes.size() + 2);
    output.writeByte((byte) type.ordinal());
    output.writeByte((byte) type.codec.version(message));
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
