package com.sedmelluq.discord.lavaplayer.format.transcoder;

import java.nio.ShortBuffer;

/**
 * Decodes one chunk of audio into internal PCM format.
 */
public interface AudioChunkDecoder {
  /**
   * @param encoded Encoded bytes
   * @param buffer Output buffer for the PCM data
   */
  void decode(byte[] encoded, ShortBuffer buffer);

  /**
   * Frees up all held resources.
   */
  void close();
}
