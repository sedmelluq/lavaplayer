package com.sedmelluq.lavaplayer.core.player.event;

import com.sedmelluq.lavaplayer.core.player.AudioPlayer;

/**
 * Event that is fired when a player is paused.
 */
public class PlayerPauseEvent extends AudioPlayerEvent {
  /**
   * @param player Audio player
   */
  public PlayerPauseEvent(AudioPlayer player) {
    super(player);
  }
}
