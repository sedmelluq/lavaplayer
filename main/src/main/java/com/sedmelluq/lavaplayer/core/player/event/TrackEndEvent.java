package com.sedmelluq.lavaplayer.core.player.event;

import com.sedmelluq.lavaplayer.core.player.track.AudioTrackEndReason;
import com.sedmelluq.lavaplayer.core.player.AudioPlayer;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrack;

/**
 * Event that is fired when an audio track ends in an audio player, either by interruption, exception or reaching the end.
 */
public class TrackEndEvent extends AudioPlayerEvent {
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
