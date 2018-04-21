package com.sedmelluq.discord.lavaplayer.container.ogg;

public class OggStreamSizeInfo {
  public final long totalBytes;
  public final long totalSamples;
  public final long firstPageOffset;
  public final long lastPageOffset;
  public final int sampleRate;

  public OggStreamSizeInfo(long totalBytes, long totalSamples, long firstPageOffset, long lastPageOffset,
                           int sampleRate) {

    this.totalBytes = totalBytes;
    this.totalSamples = totalSamples;
    this.firstPageOffset = firstPageOffset;
    this.lastPageOffset = lastPageOffset;
    this.sampleRate = sampleRate;
  }
}
