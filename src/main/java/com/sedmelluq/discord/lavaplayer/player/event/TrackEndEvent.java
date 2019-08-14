package com.sedmelluq.discord.lavaplayer.player.event;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

/**
 * Event that is fired when an audio track ends in an audio player, either by interruption, exception or reaching the end.
 */
public class TrackEndEvent extends AudioEvent {
  /**
   * Audio track that ended
   */
  public final AudioTrack track;
  /**
   * The reason why the track stopped playing
   */
  public final AudioTrackEndReason endReason;

  /**
   * @param player Audio player
   * @param track Audio track that ended
   * @param endReason The reason why the track stopped playing
   */
  public TrackEndEvent(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
    super(player);
    this.track = track;
    this.endReason = endReason;
  }
}
