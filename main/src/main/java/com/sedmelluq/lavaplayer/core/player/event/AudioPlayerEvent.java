package com.sedmelluq.lavaplayer.core.player.event;

import com.sedmelluq.lavaplayer.core.player.AudioPlayer;

/**
 * An event related to an audio player.
 */
public abstract class AudioPlayerEvent {
  /**
   * The related audio player.
   */
  public final AudioPlayer player;

  /**
   * @param player The related audio player.
   */
  public AudioPlayerEvent(AudioPlayer player) {
    this.player = player;
  }
}
