package com.sedmelluq.lavaplayer.core.player.event;

import com.sedmelluq.lavaplayer.core.player.AudioPlayer;

/**
 * Event that is fired when a player is resumed.
 */
public class PlayerResumeEvent extends AudioPlayerEvent {
  /**
   * @param player Audio player
   */
  public PlayerResumeEvent(AudioPlayer player) {
    super(player);
  }
}
