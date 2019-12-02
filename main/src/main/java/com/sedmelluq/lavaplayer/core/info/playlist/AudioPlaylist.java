package com.sedmelluq.lavaplayer.core.info.playlist;

import com.sedmelluq.lavaplayer.core.info.AudioInfoEntity;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import java.util.List;

/**
 * Playlist of audio tracks
 */
public interface AudioPlaylist extends AudioInfoEntity {
  /**
   * @return Name of the playlist
   */
  String getName();

  /**
   * @return List of tracks in the playlist
   */
  List<AudioTrackInfo> getTracks();

  /**
   * @return Track that is explicitly selected, may be null. This same instance occurs in the track list.
   */
  AudioTrackInfo getSelectedTrack();

  /**
   * @return True if the playlist was created from search results.
   */
  boolean isSearchResult();
}
