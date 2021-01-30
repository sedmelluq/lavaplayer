package com.sedmelluq.lavaplayer.core.source;

import com.sedmelluq.lavaplayer.core.container.MediaContainerRegistry;
import com.sedmelluq.lavaplayer.core.manager.AudioPlayerManager;
import com.sedmelluq.lavaplayer.core.source.bandcamp.BandcampAudioSource;
import com.sedmelluq.lavaplayer.core.source.http.HttpAudioSource;
import com.sedmelluq.lavaplayer.core.source.local.LocalAudioSource;
import com.sedmelluq.lavaplayer.core.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.lavaplayer.core.source.twitch.TwitchStreamAudioSource;
import com.sedmelluq.lavaplayer.core.source.vimeo.VimeoAudioSource;
import com.sedmelluq.lavaplayer.core.source.youtube.YoutubeAudioSource;

/**
 * A helper class for registering built-in source managers to a player manager.
 */
public class AudioSourceManagers {
  /**
   * See {@link #registerRemoteSources(AudioPlayerManager, MediaContainerRegistry)}, but with default containers.
   */
  public static void registerRemoteSources(AudioPlayerManager playerManager) {
    registerRemoteSources(playerManager, MediaContainerRegistry.DEFAULT_REGISTRY);
  }

  /**
   * Registers all built-in remote audio sources to the specified player manager. Local file audio source must be
   * registered separately.
   *
   * @param playerManager Player manager to register the source managers to
   * @param containerRegistry Media container registry to be used by any probing sources.
   */
  public static void registerRemoteSources(AudioPlayerManager playerManager, MediaContainerRegistry containerRegistry) {
    AudioSourceRegistry registry = playerManager.getSourceRegistry();
    registry.registerSource(YoutubeAudioSource.createDefault());
    registry.registerSource(SoundCloudAudioSourceManager.createDefault());
    registry.registerSource(new BandcampAudioSource());
    registry.registerSource(new VimeoAudioSource());
    registry.registerSource(new TwitchStreamAudioSource());
    registry.registerSource(new HttpAudioSource(containerRegistry));
  }

  /**
   * Registers the local file source manager to the specified player manager.
   *
   * @param playerManager Player manager to register the source manager to
   */
  public static void registerLocalSource(AudioPlayerManager playerManager) {
    registerLocalSource(playerManager, MediaContainerRegistry.DEFAULT_REGISTRY);
  }

  /**
   * Registers the local file source manager to the specified player manager.
   *
   * @param playerManager Player manager to register the source manager to
   * @param containerRegistry Media container registry to be used by the local source.
   */
  public static void registerLocalSource(AudioPlayerManager playerManager, MediaContainerRegistry containerRegistry) {
    playerManager.getSourceRegistry().registerSource(new LocalAudioSource(containerRegistry));
  }
}
