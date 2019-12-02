package com.sedmelluq.lavaplayer.core.source;

import com.sedmelluq.lavaplayer.core.info.AudioInfoEntity;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;

/**
 * Manager for a source of audio items.
 */
public interface AudioSource extends AutoCloseable {
  /**
   * Every source manager implementation should have its unique name as it is used to determine which source manager
   * should be able to decode a serialized audio track.
   *
   * @return The name of this source manager
   */
  String getName();

  AudioInfoEntity loadItem(AudioInfoRequest request);

  AudioTrackInfo decorateTrackInfo(AudioTrackInfo trackInfo);

  AudioPlayback createPlayback(AudioTrackInfo trackInfo);
}
