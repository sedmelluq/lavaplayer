package com.sedmelluq.discord.lavaplayer.track;

import java.util.List;

/**
 * The basic implementation of AudioPlaylist
 */
public class BasicAudioPlaylist implements AudioPlaylist {
  private final String name;
  private final List<AudioTrack> tracks;

  /**
   * @param name Name of the playlist
   * @param tracks List of tracks in the playlist
   */
  public BasicAudioPlaylist(String name, List<AudioTrack> tracks) {
    this.name = name;
    this.tracks = tracks;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<AudioTrack> getTracks() {
    return tracks;
  }
}
