package com.sedmelluq.discord.lavaplayer.track;

/**
 * The execution state of an audio track
 */
public enum AudioTrackState {
  INACTIVE,
  LOADING,
  PLAYING,
  SEEKING,
  STOPPING,
  FINISHED
}
