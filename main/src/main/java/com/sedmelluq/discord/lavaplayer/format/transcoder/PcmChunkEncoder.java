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
   * @param bigEndian Whether the samples are in big-endian format (as opposed to little-endian).
   */
  public PcmChunkEncoder(AudioDataFormat format, boolean bigEndian) {
    this.encoded = ByteBuffer.allocate(format.maximumChunkSize());

    if (!bigEndian) {
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
  public void encode(ShortBuffer buffer, ByteBuffer out) {
    buffer.mark();

    encodedAsShort.clear();
    encodedAsShort.put(buffer);

    out.put(encoded.array(), 0, encodedAsShort.position() * 2);
    out.flip();

    buffer.reset();
  }

  @Override
  public void close() {
    // Nothing to close here
  }
}
