package com.sedmelluq.discord.lavaplayer.format;

import com.sedmelluq.discord.lavaplayer.format.transcoder.AudioChunkDecoder;
import com.sedmelluq.discord.lavaplayer.format.transcoder.AudioChunkEncoder;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;

import java.util.Objects;

/**
 * Describes the format for audio with fixed chunk size.
 */
public abstract class AudioDataFormat {
  /**
   * Number of channels.
   */
  public final int channelCount;
  /**
   * Sample rate (frequency).
   */
  public final int sampleRate;
  /**
   * Number of samples in one chunk.
   */
  public final int chunkSampleCount;

  /**
   * @param channelCount Number of channels.
   * @param sampleRate Sample rate (frequency).
   * @param chunkSampleCount Number of samples in one chunk.
   */
  public AudioDataFormat(int channelCount, int sampleRate, int chunkSampleCount) {
    this.channelCount = channelCount;
    this.sampleRate = sampleRate;
    this.chunkSampleCount = chunkSampleCount;
  }

  /**
   * @return Total number of samples in one frame.
   */
  public int totalSampleCount() {
    return chunkSampleCount * channelCount;
  }

  /**
   * @return The duration in milliseconds of one frame in this format.
   */
  public long frameDuration() {
    return chunkSampleCount * 1000L / sampleRate;
  }

  /**
   * @return Name of the codec.
   */
  public abstract String codecName();

  /**
   * @return Byte array representing a frame of silence in this format.
   */
  public abstract byte[] silenceBytes();

  /**
   * @return Generally expected average size of a frame in this format.
   */
  public abstract int expectedChunkSize();

  /**
   * @return Maximum size of a frame in this format.
   */
  public abstract int maximumChunkSize();

  /**
   * @return Decoder to convert data in this format to short PCM.
   */
  public abstract AudioChunkDecoder createDecoder();

  /**
   * @param configuration Configuration to use for encoding.
   * @return Encoder to convert data in short PCM format to this format.
   */
  public abstract AudioChunkEncoder createEncoder(AudioConfiguration configuration);

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AudioDataFormat that = (AudioDataFormat) o;

    if (channelCount != that.channelCount) return false;
    if (sampleRate != that.sampleRate) return false;
    if (chunkSampleCount != that.chunkSampleCount) return false;
    return Objects.equals(codecName(), that.codecName());
  }

  @Override
  public int hashCode() {
    int result = channelCount;
    result = 31 * result + sampleRate;
    result = 31 * result + chunkSampleCount;
    result = 31 * result + codecName().hashCode();
    return result;
  }
}
