package com.sedmelluq.discord.lavaplayer.player.event;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

/**
 * Adapter for different event handlers as method overrides
 */
public abstract class AudioEventAdapter implements AudioEventListener {
  /**
   * @param player Audio player
   */
  public void onPlayerPause(AudioPlayer player) {
    // Adapter dummy method
  }

  /**
   * @param player Audio player
   */
  public void onPlayerResume(AudioPlayer player) {
    // Adapter dummy method
  }

  /**
   * @param player Audio player
   * @param track Audio track that started
   */
  public void onTrackStart(AudioPlayer player, AudioTrack track) {
    // Adapter dummy method
  }

  /**
   * @param player Audio player
   * @param track Audio track that ended
   * @param endReason The reason why the track stopped playing
   */
  public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
    // Adapter dummy method
  }

  /**
   * @param player Audio player
   * @param track Audio track where the exception occurred
   * @param exception The exception that occurred
   */
  public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
    // Adapter dummy method
  }

  /**
   * @param player Audio player
   * @param track Audio track where the exception occurred
   * @param thresholdMs The wait threshold that was exceeded for this event to trigger
   */
  public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
    // Adapter dummy method
  }

  @Override
  public void onEvent(AudioEvent event) {
    if (event instanceof PlayerPauseEvent) {
      onPlayerPause(event.player);
    } else if (event instanceof PlayerResumeEvent) {
      onPlayerResume(event.player);
    } else if (event instanceof TrackStartEvent) {
      onTrackStart(event.player, ((TrackStartEvent) event).track);
    } else if (event instanceof TrackEndEvent) {
      onTrackEnd(event.player, ((TrackEndEvent) event).track, ((TrackEndEvent) event).endReason);
    } else if (event instanceof TrackExceptionEvent) {
      onTrackException(event.player, ((TrackExceptionEvent) event).track, ((TrackExceptionEvent) event).exception);
    } else if (event instanceof TrackStuckEvent) {
      onTrackStuck(event.player, ((TrackStuckEvent) event).track, ((TrackStuckEvent) event).thresholdMs);
    }
  }
}
