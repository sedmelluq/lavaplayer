package com.sedmelluq.discord.lavaplayer.format.transcoder;

import java.nio.ShortBuffer;

/**
 * Encodes one chunk of audio from internal PCM format.
 */
public interface AudioChunkEncoder {
  /**
   * @param buffer Input buffer containing the PCM sampels
   * @return Encoded bytes
   */
  byte[] encode(ShortBuffer buffer);

  /**
   * Frees up all held resources.
   */
  void close();
}
