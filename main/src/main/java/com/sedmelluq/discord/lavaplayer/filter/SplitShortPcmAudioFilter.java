package com.sedmelluq.discord.lavaplayer.filter;

/**
 * Audio filter which accepts 16-bit signed PCM samples, with an array per .
 */
public interface SplitShortPcmAudioFilter extends AudioFilter {
  /**
   * @param input An array of samples for each channel
   * @param offset Offset in the array
   * @param length Length of the sequence in the array
   * @throws InterruptedException When interrupted externally (or for seek/stop).
   */
  void process(short[][] input, int offset, int length) throws InterruptedException;
}
