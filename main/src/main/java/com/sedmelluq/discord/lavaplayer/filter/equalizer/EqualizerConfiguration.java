package com.sedmelluq.discord.lavaplayer.filter.equalizer;

public class EqualizerConfiguration {
  protected final float bandMultipliers[];

  public EqualizerConfiguration(float[] bandMultipliers) {
    this.bandMultipliers = bandMultipliers;
  }

  public void setGain(int band, float value) {
    if (isValidBand(band)) {
      bandMultipliers[band] = Math.max(Math.min(value, 1.0f), -0.25f);
    }
  }

  public float getGain(int band) {
    if (isValidBand(band)) {
      return bandMultipliers[band];
    } else {
      return 0.0f;
    }
  }

  private boolean isValidBand(int band) {
    return band >= 0 && band < bandMultipliers.length;
  }
}
