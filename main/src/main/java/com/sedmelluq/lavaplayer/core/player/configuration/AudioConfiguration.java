package com.sedmelluq.lavaplayer.core.player.configuration;

import com.sedmelluq.lavaplayer.core.player.filter.PcmFilterFactory;
import com.sedmelluq.lavaplayer.core.format.AudioDataFormat;

public interface AudioConfiguration {
  int getVolumeLevel();

  long getFrameBufferDuration();

  boolean isFilterHotSwapEnabled();

  PcmFilterFactory getFilterFactory();

  ResamplingQuality getResamplingQuality();

  AudioDataFormat getOutputFormat();

  int getOpusEncodingQuality();

  boolean isUsingSeekGhosting();

  long getTrackStuckThreshold();

  long getTrackCleanupThreshold();

  <T> T getCustomOption(String name, Class<T> klass);

  /**
   * Resampling quality levels
   */
  enum ResamplingQuality {
    HIGH,
    MEDIUM,
    LOW
  }
}
