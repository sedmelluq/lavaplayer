package com.sedmelluq.discord.lavaplayer.track.playback;

/**
 * A consumer for audio frames
 */
public interface AudioFrameConsumer {
  /**
   * Consumes the frame, may block
   * @param frame The frame to consume
   * @throws InterruptedException When interrupted
   */
  void consume(AudioFrame frame) throws InterruptedException;
}
