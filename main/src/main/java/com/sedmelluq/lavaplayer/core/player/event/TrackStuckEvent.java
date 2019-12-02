package com.sedmelluq.lavaplayer.core.player.event;

import com.sedmelluq.lavaplayer.core.player.AudioPlayer;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrack;

/**
 * Event that is fired when a track was started, but no audio frames from it have arrived in a long time, specified
 * by the threshold set via AudioPlayerManager.setTrackStuckThreshold().
 */
public class TrackStuckEvent extends AudioPlayerEvent {
  /**
   * Audio track where the exception occurred
   */
  public final AudioTrack track;
  /**
   * The wait threshold that was exceeded for this event to trigger
   */
  public final long thresholdMs;

  /**
   * @param player Audio player
   * @param track Audio track where the exception occurred
   * @param thresholdMs The wait threshold that was exceeded for this event to trigger
   */
  public TrackStuckEvent(AudioPlayer player, AudioTrack track, long thresholdMs) {
    super(player);
    this.track = track;
    this.thresholdMs = thresholdMs;
  }
}
