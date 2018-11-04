package com.sedmelluq.discord.lavaplayer.filter;

/**
 * Audio filter which accepts floating point PCM samples.
 */
public interface FloatPcmAudioFilter extends AudioFilter {
  /**
   * @param input An array of samples for each channel
   * @param offset Offset in the arrays to start at
   * @param length Length of the target sequence in arrays
   * @throws InterruptedException When interrupted externally (or for seek/stop).
   */
  void process(float[][] input, int offset, int length) throws InterruptedException;
}
