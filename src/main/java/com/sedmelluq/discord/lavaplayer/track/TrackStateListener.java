package com.sedmelluq.discord.lavaplayer.track;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;

/**
 * Listener of track execution events.
 */
public interface TrackStateListener {
  /**
   * Called when an exception occurs while a track is playing or loading. This is always fatal, but it may have left
   * some data in the audio buffer which can still play until the buffer clears out.
   *
   * @param track The audio track for which the exception occurred
   * @param exception The exception that occurred
   */
  void onTrackException(AudioTrack track, FriendlyException exception);

  /**
   * Called when an exception occurs while a track is playing or loading. This is always fatal, but it may have left
   * some data in the audio buffer which can still play until the buffer clears out.
   *
   * @param track The audio track for which the exception occurred
   * @param thresholdMs The wait threshold that was exceeded for this event to trigger
   */
  void onTrackStuck(AudioTrack track, long thresholdMs);
}
