package com.sedmelluq.lavaplayer.core.player.event;

import com.sedmelluq.lavaplayer.core.player.AudioPlayer;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrack;

/**
 * Event that is fired when a track starts playing.
 */
public class TrackStartEvent extends AudioPlayerEvent {
  /**
   * Audio track that started
   */
  public final AudioTrack track;

  /**
   * @param player Audio player
   * @param track Audio track that started
   */
  public TrackStartEvent(AudioPlayer player, AudioTrack track) {
    super(player);
    this.track = track;
  }
}
