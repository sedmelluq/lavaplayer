package com.sedmelluq.discord.lavaplayer.filter;

import java.nio.ShortBuffer;

/**
 * Audio filter which accepts 16-bit signed PCM samples.
 */
public interface ShortPcmAudioFilter extends AudioFilter {
  /**
   * @param input Array of samples
   * @param offset Offset in the array
   * @param length Length of the sequence in the array
   * @throws InterruptedException When interrupted externally (or for seek/stop).
   */
  void process(short[] input, int offset, int length) throws InterruptedException;

  /**
   * @param buffer The buffer of samples
   * @throws InterruptedException When interrupted externally (or for seek/stop).
   */
  void process(ShortBuffer buffer) throws InterruptedException;
}
