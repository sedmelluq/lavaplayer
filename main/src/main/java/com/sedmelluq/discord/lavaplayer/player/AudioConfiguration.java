package com.sedmelluq.discord.lavaplayer.player;

/**
 * Configuration for audio processing.
 */
public class AudioConfiguration {
  private volatile ResamplingQuality resamplingQuality;

  /**
   * Create a new configuration with default values.
   */
  public AudioConfiguration() {
    resamplingQuality = ResamplingQuality.MEDIUM;
  }

  public ResamplingQuality getResamplingQuality() {
    return resamplingQuality;
  }

  public void setResamplingQuality(ResamplingQuality resamplingQuality) {
    this.resamplingQuality = resamplingQuality;
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
