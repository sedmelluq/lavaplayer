package com.sedmelluq.discord.lavaplayer.format.transcoder;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.natives.opus.OpusEncoder;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * Audio chunk encoder for Opus codec.
 */
public class OpusChunkEncoder implements AudioChunkEncoder {
  private final AudioDataFormat format;
  private final OpusEncoder encoder;
  private final ByteBuffer encodedBuffer;

  /**
   * @param configuration Audio configuration used for configuring the encoder
   * @param format Target audio format.
   */
  public OpusChunkEncoder(AudioConfiguration configuration, AudioDataFormat format) {
    encodedBuffer = ByteBuffer.allocateDirect(4096);
    encoder = new OpusEncoder(format.sampleRate, format.channelCount, configuration.getOpusEncodingQuality());
    this.format = format;
  }

  @Override
  public byte[] encode(ShortBuffer buffer) {
    encoder.encode(buffer, format.chunkSampleCount, encodedBuffer);

    byte[] bytes = new byte[encodedBuffer.remaining()];
    encodedBuffer.get(bytes);
    return bytes;
  }

  @Override
  public void close() {
    encoder.close();
  }
}
