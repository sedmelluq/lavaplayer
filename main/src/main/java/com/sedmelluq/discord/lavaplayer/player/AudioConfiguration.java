package com.sedmelluq.discord.lavaplayer.player;

/**
 * Configuration for audio processing.
 */
public class AudioConfiguration {
  public static final int OPUS_QUALITY_MAX = 10;

  private volatile ResamplingQuality resamplingQuality;
  private volatile int opusEncodingQuality;

  /**
   * Create a new configuration with default values.
   */
  public AudioConfiguration() {
    resamplingQuality = ResamplingQuality.LOW;
    opusEncodingQuality = OPUS_QUALITY_MAX;
  }

  public ResamplingQuality getResamplingQuality() {
    return resamplingQuality;
  }

  public void setResamplingQuality(ResamplingQuality resamplingQuality) {
    this.resamplingQuality = resamplingQuality;
  }

  public int getOpusEncodingQuality() {
    return opusEncodingQuality;
  }

  public void setOpusEncodingQuality(int opusEncodingQuality) {
    this.opusEncodingQuality = Math.max(0, Math.min(opusEncodingQuality, OPUS_QUALITY_MAX));
  }

  /**
   * Resampling quality levels
   */
  public enum ResamplingQuality {
    HIGH,
    MEDIUM,
    LOW
  }
}
