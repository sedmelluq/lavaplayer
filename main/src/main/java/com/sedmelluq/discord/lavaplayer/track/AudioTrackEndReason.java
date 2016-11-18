package com.sedmelluq.discord.lavaplayer.track;

/**
 * Reason why a track stopped playing.
 */
public enum AudioTrackEndReason {
  /**
   * This means that the track itself emitted a terminator. This is usually caused by the track reaching the end,
   * however it will also be used when it ends due to an exception.
   */
  FINISHED(true),
  /**
   * This means that the track failed to start, throwing an exception before providing any audio.
   */
  LOAD_FAILED(true),
  /**
   * The track was stopped due to the player being stopped by either calling stop() or playTrack(null).
   */
  STOPPED(false),
  /**
   * The track stopped playing because a new track started playing. Note that with this reason, the old track will still
   * play until either its buffer runs out or audio from the new track is available.
   */
  REPLACED(false),
  /**
   * The track was stopped because the cleanup threshold for the audio player was reached. This triggers when the amount
   * of time passed since the last call to AudioPlayer#provide() has reached the threshold specified in player manager
   * configuration. This may also indicate either a leaked audio player which was discarded, but not stopped.
   */
  CLEANUP(false);

  /**
   * Indicates whether a new track should be started on receiving this event. If this is false, either this event is
   * already triggered because another track started (REPLACED) or because the player is stopped (STOPPED, CLEANUP).
   */
  public final boolean mayStartNext;

  AudioTrackEndReason(boolean mayStartNext) {
    this.mayStartNext = mayStartNext;
  }
}
