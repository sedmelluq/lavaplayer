package com.sedmelluq.discord.lavaplayer.track;

/**
 * Describes a playback loop for a track
 */
public class AudioLoop {
  /**
   * Start position in milliseconds
   */
  public final long startPosition;
  /**
   * End position in milliseconds
   */
  public final long endPosition;

  /**
   * @param startPosition Start position in milliseconds
   * @param endPosition End position in milliseconds
   */
  public AudioLoop(long startPosition, long endPosition) {
    this.startPosition = startPosition;
    this.endPosition = endPosition;
  }
}
