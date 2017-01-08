package com.sedmelluq.discord.lavaplayer.remote.message;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Codec for encoding and decoding remote messages.
 *
 * @param <T> The message class
 */
public interface RemoteMessageCodec<T extends RemoteMessage> {
  /**
   * @return The class of the message this codec works with
   */
  Class<T> getMessageClass();

  /**
   * @return Latest version of this codec.
   */
  int version();

  /**
   * Encode the message to the specified output.
   *
   * @param out The output stream
   * @param message The message to encode
   * @throws IOException When an IO error occurs
   */
  void encode(DataOutput out, T message) throws IOException;

  /**
   * Decode a message from the specified input.
   *
   * @param in The input stream
   * @param version Version of the message
   * @return The decoded message
   * @throws IOException When an IO error occurs
   */
  T decode(DataInput in, int version) throws IOException;
}
