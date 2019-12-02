package com.sedmelluq.lavaplayer.core.player;

import com.sedmelluq.lavaplayer.core.player.track.AudioTrackFactory;
import com.sedmelluq.lavaplayer.core.player.configuration.AudioConfiguration;

public class DefaultAudioPlayerFactory implements AudioPlayerFactory {
  private final AudioPlayerLifecycleManager lifecycleManager = new AudioPlayerLifecycleManager();

  @Override
  public AudioPlayer create(AudioTrackFactory trackFactory, AudioConfiguration configuration) {
    AudioPlayer player = new DefaultAudioPlayer(trackFactory, configuration);
    player.addListener(lifecycleManager);
    return player;
  }

  @Override
  public void close() {
    lifecycleManager.close();
  }
}
