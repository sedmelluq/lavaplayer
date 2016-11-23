package com.sedmelluq.discord.lavaplayer.track;

import java.util.List;

/**
 * Playlist of audio tracks
 */
public interface AudioPlaylist extends AudioItem {
  /**
   * @return Name of the playlist
   */
  String getName();

  /**
   * @return List of tracks in the playlist
   */
  List<AudioTrack> getTracks();

  /**
   * @return Track that is explicitly selected, may be null. This same instance occurs in the track list.
   */
  AudioTrack getSelectedTrack();

  /**
   * @return True if the playlist was created from search results.
   */
  boolean isSearchResult();
}
