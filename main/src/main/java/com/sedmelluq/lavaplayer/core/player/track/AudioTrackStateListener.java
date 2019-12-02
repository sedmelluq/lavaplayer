package com.sedmelluq.lavaplayer.core.player.track;

import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;

public interface AudioTrackStateListener {
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
