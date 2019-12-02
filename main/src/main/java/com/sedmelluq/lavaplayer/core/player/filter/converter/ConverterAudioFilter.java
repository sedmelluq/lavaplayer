package com.sedmelluq.lavaplayer.core.player.filter.converter;

import com.sedmelluq.lavaplayer.core.player.filter.UniversalPcmAudioFilter;

/**
 * Base class for converter filters which have no internal state.
 */
public abstract class ConverterAudioFilter implements UniversalPcmAudioFilter {
  protected static final int BUFFER_SIZE = 4096;

  @Override
  public void seekPerformed(long requestedTime, long providedTime) {
    // Nothing to do.
  }

  @Override
  public void flush() throws InterruptedException {
    // Nothing to do.
  }

  @Override
  public void close() {
    // Nothing to do.
  }

  protected static short floatToShort(float value) {
    return (short) (value * 32768.0f);
  }
}
