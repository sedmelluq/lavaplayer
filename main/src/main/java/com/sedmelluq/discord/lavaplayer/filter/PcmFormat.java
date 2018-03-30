package com.sedmelluq.discord.lavaplayer.filter;

public class PcmFormat {
  /**
   * Number of channels.
   */
  public final int channelCount;
  /**
   * Sample rate (frequency).
   */
  public final int sampleRate;

  public PcmFormat(int channelCount, int sampleRate) {
    this.channelCount = channelCount;
    this.sampleRate = sampleRate;
  }
}
