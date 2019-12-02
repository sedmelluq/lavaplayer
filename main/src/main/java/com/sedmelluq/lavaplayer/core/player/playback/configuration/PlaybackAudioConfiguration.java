package com.sedmelluq.lavaplayer.core.player.playback.configuration;

import com.sedmelluq.lavaplayer.core.player.configuration.AudioConfiguration;
import com.sedmelluq.lavaplayer.core.player.configuration.ExtendedAudioConfiguration;
import com.sedmelluq.lavaplayer.core.format.AudioDataFormat;

public class PlaybackAudioConfiguration extends ExtendedAudioConfiguration {
  private final AudioDataFormat frozenOutputFormat;
  private final long frozenFrameBufferDuration;
  private final boolean frozenFilterHotSwapEnabled;
  private final boolean frozenUseSeekGhosting;

  public PlaybackAudioConfiguration(AudioConfiguration delegate) {
    super(delegate);
    this.frozenOutputFormat = delegate.getOutputFormat();
    this.frozenFrameBufferDuration = delegate.getFrameBufferDuration();
    this.frozenFilterHotSwapEnabled = delegate.isFilterHotSwapEnabled();
    this.frozenUseSeekGhosting = delegate.isUsingSeekGhosting();
  }

  @Override
  public long getFrameBufferDuration() {
    return frozenFrameBufferDuration;
  }

  @Override
  public boolean isFilterHotSwapEnabled() {
    return frozenFilterHotSwapEnabled;
  }

  @Override
  public AudioDataFormat getOutputFormat() {
    return frozenOutputFormat;
  }

  @Override
  public boolean isUsingSeekGhosting() {
    return frozenUseSeekGhosting;
  }
}
