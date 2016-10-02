package com.sedmelluq.discord.lavaplayer.source;

import com.sedmelluq.discord.lavaplayer.track.AudioItem;

/**
 * Manager for a possible source of audio items.
 */
public interface AudioSourceManager {
  /**
   * Returns an audio track for the input string. It should return null if it can immediately detect that there is no
   * track for this identifier for this source. If checking that requires more expensive operations, then it should
   * return a track instance and check that in InternalAudioTrack#loadTrackInfo.
   *
   * @param identifier The identifier which the source manager should find the track with
   * @return The loaded item or null on unrecognized identifier
   */
  AudioItem loadItem(String identifier);
}
