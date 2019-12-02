package com.sedmelluq.lavaplayer.core.player.playback;

import com.sedmelluq.lavaplayer.core.player.configuration.AudioConfiguration;
import com.sedmelluq.lavaplayer.core.player.frame.AudioFrameBuffer;

public interface AudioPlaybackContext {
  AudioConfiguration getConfiguration();

  AudioFrameBuffer getFrameBuffer();
}
