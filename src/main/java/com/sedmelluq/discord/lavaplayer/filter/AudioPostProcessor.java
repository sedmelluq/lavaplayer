package com.sedmelluq.discord.lavaplayer.filter;

import java.nio.ShortBuffer;

/**
 * Audio chunk post processor.
 */
public interface AudioPostProcessor {
  /**
   * Receives chunk buffer in its final PCM format with the sample count, sample rate and channel count matching that of
   * the output format.
   *
   * @param timecode Absolute starting timecode of the chunk in milliseconds
   * @param buffer PCM buffer of samples in the chunk
   * @throws InterruptedException When interrupted externally (or for seek/stop).
   */
  void process(long timecode, ShortBuffer buffer) throws InterruptedException;

  /**
   * Frees up all resources this processor is holding internally.
   */
  void close();
}
