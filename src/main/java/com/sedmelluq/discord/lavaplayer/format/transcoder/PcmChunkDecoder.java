package com.sedmelluq.discord.lavaplayer.format.transcoder;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Audio chunk decoder for PCM data.
 */
public class PcmChunkDecoder implements AudioChunkDecoder {
  private final ByteBuffer encodedAsByte;
  private final ShortBuffer encodedAsShort;

  /**
   * @param format Source audio format.
   * @param bigEndian Whether the samples are in big-endian format (as opposed to little-endian).
   */
  public PcmChunkDecoder(AudioDataFormat format, boolean bigEndian) {
    this.encodedAsByte = ByteBuffer.allocate(format.maximumChunkSize());

    if (!bigEndian) {
      encodedAsByte.order(ByteOrder.LITTLE_ENDIAN);
    }

    this.encodedAsShort = encodedAsByte.asShortBuffer();
  }

  @Override
  public void decode(byte[] encoded, ShortBuffer buffer) {
    buffer.clear();

    encodedAsByte.clear();
    encodedAsByte.put(encoded);

    encodedAsShort.clear();
    encodedAsShort.limit(encodedAsByte.position() / 2);

    buffer.put(encodedAsShort);
    buffer.rewind();
  }

  @Override
  public void close() {
    // Nothing to close here
  }
}
