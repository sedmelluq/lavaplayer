package com.sedmelluq.discord.lavaplayer.track;

import java.util.List;

/**
 * The basic implementation of AudioPlaylist
 */
public class BasicAudioPlaylist implements AudioPlaylist {
  private final String name;
  private final List<AudioTrack> tracks;
  private final AudioTrack selectedTrack;

  /**
   * @param name Name of the playlist
   * @param tracks List of tracks in the playlist
   * @param selectedTrack Track that is explicitly selected
   */
  public BasicAudioPlaylist(String name, List<AudioTrack> tracks, AudioTrack selectedTrack) {
    this.name = name;
    this.tracks = tracks;
    this.selectedTrack = selectedTrack;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<AudioTrack> getTracks() {
    return tracks;
  }

  @Override
  public AudioTrack getSelectedTrack() {
    return selectedTrack;
  }
}
