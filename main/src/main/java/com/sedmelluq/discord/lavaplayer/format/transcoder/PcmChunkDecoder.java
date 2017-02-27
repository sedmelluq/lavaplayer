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
   */
  public PcmChunkDecoder(AudioDataFormat format) {
    this.encodedAsByte = ByteBuffer.allocate(format.bufferSize(2));

    if (format.codec == AudioDataFormat.Codec.PCM_S16_LE) {
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
