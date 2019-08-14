package com.sedmelluq.discord.lavaplayer.track.playback;

/**
 * Interface for classes which can rebuild audio frames.
 */
public interface AudioFrameRebuilder {
  /**
   * Rebuilds a frame (for example by reencoding)
   * @param frame The audio frame
   * @return The new frame (may be the same as input)
   */
  AudioFrame rebuild(AudioFrame frame);
}
