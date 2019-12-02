package com.sedmelluq.lavaplayer.core.info.loader;

import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import com.sedmelluq.lavaplayer.core.info.playlist.AudioPlaylist;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;

/**
 * Handles the result of loading an item from an audio player manager.
 */
public interface AudioInfoResponseHandler {
  /**
   * Called when the requested item is a track and it was successfully loaded.
   * @param track The loaded track
   */
  void trackLoaded(AudioTrackInfo track);

  /**
   * Called when the requested item is a playlist and it was successfully loaded.
   * @param playlist The loaded playlist
   */
  void playlistLoaded(AudioPlaylist playlist);

  /**
   * Called when there were no items found by the specified identifier.
   */
  void noMatches();

  /**
   * Called when loading an item failed with an exception.
   * @param exception The exception that was thrown
   */
  void loadFailed(FriendlyException exception);
}
