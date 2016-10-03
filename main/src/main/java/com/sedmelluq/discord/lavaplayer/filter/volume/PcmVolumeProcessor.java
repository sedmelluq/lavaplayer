package com.sedmelluq.discord.lavaplayer.filter.volume;

import java.nio.ShortBuffer;

/**
 * Class used to apply a volume level to short PCM buffers
 */
public class PcmVolumeProcessor {
  private int currentVolume = -1;
  private int integerMultiplier;

  /**
   * @param initialVolume Initial volume level (only useful for getLastVolume() as specified with each call)
   */
  public PcmVolumeProcessor(int initialVolume) {
    setupMultipliers(initialVolume);
  }

  /**
   * @return Last volume level used with this processor
   */
  public int getLastVolume() {
    return currentVolume;
  }

  /**
   * @param initialVolume The input volume of the samples
   * @param targetVolume The target volume of the samples
   * @param buffer The buffer containing the samples
   */
  public void applyVolume(int initialVolume, int targetVolume, ShortBuffer buffer) {
    if (initialVolume != 100) {
      setupMultipliers(initialVolume);
      unapplyCurrentVolume(buffer);
    }

    setupMultipliers(targetVolume);
    applyCurrentVolume(buffer);
  }

  private void setupMultipliers(int activeVolume) {
    if (currentVolume != activeVolume) {
      currentVolume = activeVolume;
      float floatMultiplier = (float) Math.tan(activeVolume * 0.0079f);
      integerMultiplier = (int) (floatMultiplier * 10000);
    }
  }

  private void applyCurrentVolume(ShortBuffer buffer) {
    if (currentVolume == 100) {
      return;
    }

    int endOffset = buffer.limit();

    for (int i = buffer.position(); i < endOffset; i++) {
      int value = buffer.get(i) * integerMultiplier / 10000;
      buffer.put(i, (short) Math.max(-32767, Math.min(32767, value)));
    }
  }

  private void unapplyCurrentVolume(ShortBuffer buffer) {
    if (integerMultiplier == 0) {
      return;
    }

    int endOffset = buffer.limit();

    for (int i = buffer.position(); i < endOffset; i++) {
      int value = buffer.get(i) * 10000 / integerMultiplier;
      buffer.put(i, (short) Math.max(-32767, Math.min(32767, value)));
    }
  }
}
