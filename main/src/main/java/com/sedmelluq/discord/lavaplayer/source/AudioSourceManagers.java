package com.sedmelluq.discord.lavaplayer.source;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;

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
    playerManager.registerSourceManager(new YoutubeAudioSourceManager(true));
    playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
    playerManager.registerSourceManager(new BandcampAudioSourceManager());
    playerManager.registerSourceManager(new VimeoAudioSourceManager());
    playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
    playerManager.registerSourceManager(new BeamAudioSourceManager());
    playerManager.registerSourceManager(new HttpAudioSourceManager(containerRegistry));
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
    playerManager.registerSourceManager(new LocalAudioSourceManager(containerRegistry));
  }
}
