package com.sedmelluq.discord.lavaplayer.format;

import com.sedmelluq.discord.lavaplayer.format.transcoder.AudioChunkDecoder;
import com.sedmelluq.discord.lavaplayer.format.transcoder.AudioChunkEncoder;
import com.sedmelluq.discord.lavaplayer.format.transcoder.PcmChunkDecoder;
import com.sedmelluq.discord.lavaplayer.format.transcoder.PcmChunkEncoder;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;

/**
 * An {@link AudioDataFormat} for 16-bit signed PCM.
 */
public class Pcm16AudioDataFormat extends AudioDataFormat {
  public static final String CODEC_NAME_BE = "PCM_S16_BE";
  public static final String CODEC_NAME_LE = "PCM_S16_LE";

  private final boolean bigEndian;
  private final byte[] silenceBytes;

  /**
   * @param channelCount     Number of channels.
   * @param sampleRate       Sample rate (frequency).
   * @param chunkSampleCount Number of samples in one chunk.
   * @param bigEndian        Whether the samples are in big-endian format (as opposed to little-endian).
   */
  public Pcm16AudioDataFormat(int channelCount, int sampleRate, int chunkSampleCount, boolean bigEndian) {
    super(channelCount, sampleRate, chunkSampleCount);
    this.bigEndian = bigEndian;
    this.silenceBytes = new byte[channelCount * chunkSampleCount * 2];
  }

  @Override
  public String codecName() {
    return CODEC_NAME_BE;
  }

  @Override
  public byte[] silenceBytes() {
    return silenceBytes;
  }

  @Override
  public int expectedChunkSize() {
    return silenceBytes.length;
  }

  @Override
  public int maximumChunkSize() {
    return silenceBytes.length;
  }

  @Override
  public AudioChunkDecoder createDecoder() {
    return new PcmChunkDecoder(this, bigEndian);
  }

  @Override
  public AudioChunkEncoder createEncoder(AudioConfiguration configuration) {
    return new PcmChunkEncoder(this, bigEndian);
  }

  @Override
  public boolean equals(Object o) {
    return this == o || o != null && getClass() == o.getClass() && super.equals(o);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
