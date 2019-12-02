package com.sedmelluq.lavaplayer.core.player;

public interface InternalAudioPlayer extends AudioPlayer {
  /**
   * Check if the player should be "cleaned up" - stopped due to nothing using it, with the given threshold.
   * @param threshold Threshold in milliseconds to use
   */
  void checkCleanup(long threshold);
}
