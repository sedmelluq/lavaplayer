package com.sedmelluq.discord.lavaplayer.container.matroska.format;

/**
 * Matroska file cue point. Provides the offsets at a specific timecode for each track
 */
public class MatroskaCuePoint {
  /**
   * Timecode using the file timescale
   */
  public final long timecode;
  /**
   * Absolute offset to the cluster
   */
  public final long[] trackClusterOffsets;

  /**
   * @param timecode Timecode using the file timescale
   * @param trackClusterOffsets Absolute offset to the cluster
   */
  public MatroskaCuePoint(long timecode, long[] trackClusterOffsets) {
    this.timecode = timecode;
    this.trackClusterOffsets = trackClusterOffsets;
  }
}
