package com.sedmelluq.discord.lavaplayer.format.transcoder;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Audio chunk encoder for PCM data.
 */
public class PcmChunkEncoder implements AudioChunkEncoder {
  private final ByteBuffer encoded;
  private final ShortBuffer encodedAsShort;

  /**
   * @param format Target audio format.
   */
  public PcmChunkEncoder(AudioDataFormat format) {
    this.encoded = ByteBuffer.allocate(format.bufferSize(2));

    if (format.codec == AudioDataFormat.Codec.PCM_S16_LE) {
      encoded.order(ByteOrder.LITTLE_ENDIAN);
    }

    this.encodedAsShort = encoded.asShortBuffer();
  }

  @Override
  public byte[] encode(ShortBuffer buffer) {
    buffer.mark();

    encodedAsShort.clear();
    encodedAsShort.put(buffer);

    encoded.clear();
    encoded.limit(encodedAsShort.position() * 2);

    byte[] encodedBytes = new byte[encoded.remaining()];
    encoded.get(encodedBytes);

    buffer.reset();
    return encodedBytes;
  }

  @Override
  public void close() {
    // Nothing to close here
  }
}
