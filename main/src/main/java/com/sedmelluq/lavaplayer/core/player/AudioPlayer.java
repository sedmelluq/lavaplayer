package com.sedmelluq.lavaplayer.core.player;

import com.sedmelluq.lavaplayer.core.player.configuration.OverlayAudioConfiguration;
import com.sedmelluq.lavaplayer.core.player.event.AudioPlayerEventListener;
import com.sedmelluq.lavaplayer.core.player.frame.AudioFrameProvider;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrack;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrackRequest;

/**
 * An audio player that is capable of playing audio tracks and provides audio frames from the currently playing track.
 */
public interface AudioPlayer extends AudioFrameProvider, AutoCloseable {
  /**
   * @return Currently playing track
   */
  AudioTrack getPlayingTrack();

  AudioTrack playTrack(AudioTrackRequest request);

  OverlayAudioConfiguration getConfiguration();

  /**
   * Stop currently playing track.
   */
  void stopTrack();

  /**
   * @return Whether the player is paused
   */
  boolean isPaused();

  /**
   * @param value True to pause, false to resume
   */
  void setPaused(boolean value);

  /**
   * Add a listener to events from this player.
   * @param listener New listener
   */
  void addListener(AudioPlayerEventListener listener);

  /**
   * Remove an attached listener using identity comparison.
   * @param listener The listener to remove
   */
  void removeListener(AudioPlayerEventListener listener);

  void checkCleanup();
}
