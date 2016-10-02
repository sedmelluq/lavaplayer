package com.sedmelluq.discord.lavaplayer.track.playback;

/**
 * A provider for audio frames
 */
public interface AudioFrameProvider {
  /**
   * @return Provided frame, or null if none available
   */
  AudioFrame provide();
}
