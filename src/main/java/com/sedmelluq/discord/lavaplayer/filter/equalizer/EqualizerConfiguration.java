package com.sedmelluq.discord.lavaplayer.filter.equalizer;

/**
 * Holder of equalizer configuration.
 */
public class EqualizerConfiguration {
  protected final float[] bandMultipliers;

  /**
   * @param bandMultipliers The band multiplier values. Keeps using this array internally, so the values can be changed
   *                        externally.
   */
  public EqualizerConfiguration(float[] bandMultipliers) {
    this.bandMultipliers = bandMultipliers;
  }

  /**
   * @param band The index of the band. If this is not a valid band index, the method has no effect.
   * @param value The multiplier for this band. Default value is 0. Valid values are from -0.25 to 1. -0.25 means that
   *              the given frequency is completely muted and 0.25 means it is doubled. Note that this may change the
   *              volume of the output.
   */
  public void setGain(int band, float value) {
    if (isValidBand(band)) {
      bandMultipliers[band] = Math.max(Math.min(value, 1.0f), -0.25f);
    }
  }

  /**
   * @param band The index of the band.
   * @return The multiplier for this band. Default value is 0.
   */
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
