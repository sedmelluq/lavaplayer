package com.sedmelluq.discord.lavaplayer.format.transcoder;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * Encodes one chunk of audio from internal PCM format.
 */
public interface AudioChunkEncoder {
  /**
   * @param buffer Input buffer containing the PCM samples.
   * @return Encoded bytes
   */
  byte[] encode(ShortBuffer buffer);

  /**
   * @param buffer Input buffer containing the PCM samples.
   * @param out Output buffer to store the encoded bytes in
   */
  void encode(ShortBuffer buffer, ByteBuffer out);

  /**
   * Frees up all held resources.
   */
  void close();
}
