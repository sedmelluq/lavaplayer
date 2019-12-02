package com.sedmelluq.lavaplayer.core.info.playlist;

import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import java.util.List;

/**
 * The basic implementation of AudioPlaylist
 */
public class BasicAudioPlaylist implements AudioPlaylist {
  private final String name;
  private final List<AudioTrackInfo> tracks;
  private final AudioTrackInfo selectedTrack;
  private final boolean isSearchResult;

  /**
   * @param name Name of the playlist
   * @param tracks List of tracks in the playlist
   * @param selectedTrack Track that is explicitly selected
   * @param isSearchResult True if the playlist was created from search results
   */
  public BasicAudioPlaylist(String name, List<AudioTrackInfo> tracks, AudioTrackInfo selectedTrack,
                            boolean isSearchResult) {

    this.name = name;
    this.tracks = tracks;
    this.selectedTrack = selectedTrack;
    this.isSearchResult = isSearchResult;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<AudioTrackInfo> getTracks() {
    return tracks;
  }

  @Override
  public AudioTrackInfo getSelectedTrack() {
    return selectedTrack;
  }

  @Override
  public boolean isSearchResult() {
    return isSearchResult;
  }
}
