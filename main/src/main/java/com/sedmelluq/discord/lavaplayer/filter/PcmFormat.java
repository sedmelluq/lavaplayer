package com.sedmelluq.discord.lavaplayer.filter;

/**
 * Describes the properties of PCM data.
 */
public class PcmFormat {
  /**
   * Number of channels.
   */
  public final int channelCount;
  /**
   * Sample rate (frequency).
   */
  public final int sampleRate;

  /**
   * @param channelCount See {@link #channelCount}.
   * @param sampleRate See {@link #sampleRate}.
   */
  public PcmFormat(int channelCount, int sampleRate) {
    this.channelCount = channelCount;
    this.sampleRate = sampleRate;
  }
}
