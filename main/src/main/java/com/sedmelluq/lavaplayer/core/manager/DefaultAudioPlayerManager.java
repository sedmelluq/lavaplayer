package com.sedmelluq.lavaplayer.core.manager;

import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.loader.AudioInfoRequestHandler;
import com.sedmelluq.lavaplayer.core.info.loader.DefaultAudioInfoRequestHandler;
import com.sedmelluq.lavaplayer.core.source.AudioSourceRegistry;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrackFactory;
import com.sedmelluq.lavaplayer.core.player.AudioPlayer;
import com.sedmelluq.lavaplayer.core.player.AudioPlayerFactory;
import com.sedmelluq.lavaplayer.core.player.DefaultAudioPlayerFactory;
import com.sedmelluq.lavaplayer.core.player.configuration.MutableAudioConfiguration;
import com.sedmelluq.lavaplayer.core.player.configuration.RootAudioConfiguration;
import com.sedmelluq.lavaplayer.core.player.playback.PlaybackAudioTrackFactory;
import com.sedmelluq.lavaplayer.core.source.DefaultAudioSourceRegistry;
import java.util.concurrent.Future;

public class DefaultAudioPlayerManager implements AudioPlayerManager {
  private final MutableAudioConfiguration configuration;
  private final AudioSourceRegistry sourceRegistry;
  private final AudioTrackFactory trackFactory;
  private final AudioPlayerFactory playerFactory;
  private final AudioInfoRequestHandler infoLoader;

  public static DefaultAudioPlayerManager createDefault() {
    AudioSourceRegistry sourceRegistry = new DefaultAudioSourceRegistry();

    return new DefaultAudioPlayerManager(
        new RootAudioConfiguration(),
        sourceRegistry,
        new PlaybackAudioTrackFactory(sourceRegistry),
        new DefaultAudioPlayerFactory(),
        new DefaultAudioInfoRequestHandler(sourceRegistry)
    );
  }

  public DefaultAudioPlayerManager(
      MutableAudioConfiguration configuration,
      AudioSourceRegistry sourceRegistry,
      AudioTrackFactory trackFactory,
      AudioPlayerFactory playerFactory,
      AudioInfoRequestHandler infoLoader
  ) {
    this.configuration = configuration;
    this.sourceRegistry = sourceRegistry;
    this.trackFactory = trackFactory;
    this.playerFactory = playerFactory;
    this.infoLoader = infoLoader;
  }

  @Override
  public AudioSourceRegistry getSourceRegistry() {
    return sourceRegistry;
  }

  @Override
  public Future<Void> requestInfo(AudioInfoRequest request) {
    return infoLoader.request(request);
  }

  @Override
  public MutableAudioConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public AudioPlayer createPlayer() {
    return playerFactory.create(trackFactory, configuration);
  }

  @Override
  public void close() throws Exception {
    sourceRegistry.close();
    trackFactory.close();
    playerFactory.close();
    infoLoader.close();
  }
}
