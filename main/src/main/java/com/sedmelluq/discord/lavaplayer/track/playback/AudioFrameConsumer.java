package com.sedmelluq.discord.lavaplayer.track.playback;

/**
 * A consumer for audio frames
 */
public interface AudioFrameConsumer {
  /**
   * Consumes the frame, may block
   * @param frame The frame to consume
   * @throws InterruptedException When interrupted externally (or for seek/stop).
   */
  void consume(AudioFrame frame) throws InterruptedException;

  /**
   * Rebuild all caches frames
   * @param rebuilder The rebuilder to use
   */
  void rebuild(AudioFrameRebuilder rebuilder);
}
