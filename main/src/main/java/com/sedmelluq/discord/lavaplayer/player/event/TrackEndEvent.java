package com.sedmelluq.discord.lavaplayer.player.event;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

/**
 * Event that is fired when an audio track ends in an audio player, either by interruption, exception or reaching the end.
 */
public class TrackEndEvent extends AudioEvent {
  /**
   * Audio track that ended
   */
  public final AudioTrack track;
  /**
   * Whether the track was interrupted via stop or new track
   */
  public final boolean interrupted;

  /**
   * @param player Audio player
   * @param track Audio track that ended
   * @param interrupted Whether the track was interrupted via stop or new track
   */
  public TrackEndEvent(AudioPlayer player, AudioTrack track, boolean interrupted) {
    super(player);
    this.track = track;
    this.interrupted = interrupted;
  }
}
