package com.sedmelluq.lavaplayer.core.manager;

import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.source.AudioSourceRegistry;
import com.sedmelluq.lavaplayer.core.player.AudioPlayer;
import com.sedmelluq.lavaplayer.core.player.configuration.MutableAudioConfiguration;
import java.util.concurrent.Future;

/**
 * Audio player manager which is used for creating audio players and loading tracks and playlists.
 */
public interface AudioPlayerManager extends AutoCloseable {
  AudioSourceRegistry getSourceRegistry();

  Future<Void> requestInfo(AudioInfoRequest request);

  /**
   * @return Audio processing configuration used for tracks executed by this manager.
   */
  MutableAudioConfiguration getConfiguration();

  /**
   * @return New audio player.
   */
  AudioPlayer createPlayer();
}
