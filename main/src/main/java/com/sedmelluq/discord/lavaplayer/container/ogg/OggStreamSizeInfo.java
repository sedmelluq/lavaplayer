package com.sedmelluq.discord.lavaplayer.container.ogg;

/**
 * Describes the size information of an OGG stream.
 */
public class OggStreamSizeInfo {
  /**
   * Total number of bytes in the stream.
   */
  public final long totalBytes;
  /**
   * Total number of samples in the stream.
   */
  public final long totalSamples;
  /**
   * Absolute offset of the first page in the stream.
   */
  public final long firstPageOffset;
  /**
   * Absolute offset of the last page in the stream.
   */
  public final long lastPageOffset;
  /**
   * Sample rate of the track in this stream, useful for calculating duration in milliseconds.
   */
  public final int sampleRate;

  /**
   * @param totalBytes See {@link #totalBytes}.
   * @param totalSamples See {@link #totalSamples}.
   * @param firstPageOffset See {@link #firstPageOffset}.
   * @param lastPageOffset See {@link #lastPageOffset}.
   * @param sampleRate See {@link #sampleRate}.
   */
  public OggStreamSizeInfo(long totalBytes, long totalSamples, long firstPageOffset, long lastPageOffset,
                           int sampleRate) {

    this.totalBytes = totalBytes;
    this.totalSamples = totalSamples;
    this.firstPageOffset = firstPageOffset;
    this.lastPageOffset = lastPageOffset;
    this.sampleRate = sampleRate;
  }
}
