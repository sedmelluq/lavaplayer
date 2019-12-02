package com.sedmelluq.lavaplayer.core.player.configuration;

import com.sedmelluq.lavaplayer.core.player.filter.PcmFilterFactory;
import com.sedmelluq.lavaplayer.core.format.AudioDataFormat;

public abstract class ExtendedAudioConfiguration implements AudioConfiguration {
  private final AudioConfiguration delegate;

  public ExtendedAudioConfiguration(AudioConfiguration delegate) {
    this.delegate = delegate;
  }

  @Override
  public int getVolumeLevel() {
    return delegate.getVolumeLevel();
  }

  @Override
  public long getFrameBufferDuration() {
    return delegate.getFrameBufferDuration();
  }

  @Override
  public boolean isFilterHotSwapEnabled() {
    return delegate.isFilterHotSwapEnabled();
  }

  @Override
  public PcmFilterFactory getFilterFactory() {
    return delegate.getFilterFactory();
  }

  @Override
  public ResamplingQuality getResamplingQuality() {
    return delegate.getResamplingQuality();
  }

  @Override
  public AudioDataFormat getOutputFormat() {
    return delegate.getOutputFormat();
  }

  @Override
  public int getOpusEncodingQuality() {
    return delegate.getOpusEncodingQuality();
  }

  @Override
  public boolean isUsingSeekGhosting() {
    return delegate.isUsingSeekGhosting();
  }

  @Override
  public long getTrackStuckThreshold() {
    return delegate.getTrackStuckThreshold();
  }

  @Override
  public long getTrackCleanupThreshold() {
    return delegate.getTrackCleanupThreshold();
  }

  @Override
  public <T> T getCustomOption(String name, Class<T> klass) {
    return delegate.getCustomOption(name, klass);
  }
}
