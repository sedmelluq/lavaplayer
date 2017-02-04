package com.sedmelluq.discord.lavaplayer.format;

/**
 * Describes the format for audio with fixed chunk size.
 */
public class AudioDataFormat {
  private static final byte[] SILENT_OPUS_FRAME = new byte[] {(byte) 0xFC, (byte) 0xFF, (byte) 0xFE};


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
   * Codec used to produce the raw buffer.
   */
  public final Codec codec;
  /**
   * Bytes representing a silent chunk with this format.
   */
  public final byte[] silence;

  /**
   * @param channelCount Number of channels.
   * @param sampleRate Sample rate (frequency).
   * @param chunkSampleCount Number of samples in one chunk.
   * @param codec Codec used to produce the raw buffer.
   */
  public AudioDataFormat(int channelCount, int sampleRate, int chunkSampleCount, Codec codec) {
    this.channelCount = channelCount;
    this.sampleRate = sampleRate;
    this.chunkSampleCount = chunkSampleCount;
    this.codec = codec;
    this.silence = produceSilence();
  }

  /**
   * @param sampleSize Size per sample.
   * @return Size of a buffer that can fit one chunk in this format, assuming fixed sample size.
   */
  public int bufferSize(int sampleSize) {
    return chunkSampleCount * channelCount * sampleSize;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AudioDataFormat that = (AudioDataFormat) o;

    if (channelCount != that.channelCount) return false;
    if (sampleRate != that.sampleRate) return false;
    if (chunkSampleCount != that.chunkSampleCount) return false;
    return codec == that.codec;
  }

  @Override
  public int hashCode() {
    int result = channelCount;
    result = 31 * result + sampleRate;
    result = 31 * result + chunkSampleCount;
    result = 31 * result + codec.hashCode();
    return result;
  }

  private byte[] produceSilence() {
    if (codec == Codec.OPUS) {
      return SILENT_OPUS_FRAME;
    } else {
      return new byte[bufferSize(2)];
    }
  }

  /**
   * Codec of the audio.
   */
  public enum Codec {
    /**
     * Opus codec.
     */
    OPUS,
    /**
     * Signed 16-bit little-endian PCM
     */
    PCM_S16_LE,
    /**
     * Signed 16-bit big-endian PCM
     */
    PCM_S16_BE
  }
}
