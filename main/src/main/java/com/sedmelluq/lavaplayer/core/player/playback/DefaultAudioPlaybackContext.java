package com.sedmelluq.lavaplayer.core.player.playback;

import com.sedmelluq.lavaplayer.core.player.frame.AudioFrameBuffer;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackContext;
import com.sedmelluq.lavaplayer.core.player.configuration.AudioConfiguration;

public class DefaultAudioPlaybackContext implements AudioPlaybackContext {
  private final AudioConfiguration configuration;
  private final AudioFrameBuffer frameBuffer;

  public DefaultAudioPlaybackContext(AudioConfiguration configuration, AudioFrameBuffer frameBuffer) {
    this.configuration = configuration;
    this.frameBuffer = frameBuffer;
  }

  @Override
  public AudioConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public AudioFrameBuffer getFrameBuffer() {
    return frameBuffer;
  }
}
