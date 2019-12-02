package com.sedmelluq.lavaplayer.core.player;

import com.sedmelluq.lavaplayer.core.player.track.AudioTrackFactory;
import com.sedmelluq.lavaplayer.core.player.configuration.AudioConfiguration;

public interface AudioPlayerFactory extends AutoCloseable {
  AudioPlayer create(AudioTrackFactory trackFactory, AudioConfiguration configuration);
}
