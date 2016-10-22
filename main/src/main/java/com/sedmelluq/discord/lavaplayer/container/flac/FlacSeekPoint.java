package com.sedmelluq.discord.lavaplayer.container.flac;

/**
 * FLAC seek point info. Field descriptions are from:
 * https://xiph.org/flac/format.html#seekpoint
 *
 * - For placeholder points, the second and third field values are undefined.
 * - Seek points within a table must be sorted in ascending order by sample number.
 * - Seek points within a table must be unique by sample number, with the exception of placeholder points.
 * - The previous two notes imply that there may be any number of placeholder points, but they must all occur at the end
 *   of the table.
 */
public class FlacSeekPoint {
  public static final int LENGTH = 18;

  /**
   * Sample number of first sample in the target frame, or 0xFFFFFFFFFFFFFFFF for a placeholder point.
   */
  public final long sampleIndex;

  /**
   * Offset (in bytes) from the first byte of the first frame header to the first byte of the target frame's header.
   */
  public final long byteOffset;

  /**
   * Number of samples in the target frame.
   */
  public final int sampleCount;

  /**
   * @param sampleIndex Index of the first sample in the frame
   * @param byteOffset Offset in bytes from first frame start to target frame start
   * @param sampleCount Number of samples in the frame
   */
  public FlacSeekPoint(long sampleIndex, long byteOffset, int sampleCount) {
    this.sampleIndex = sampleIndex;
    this.byteOffset = byteOffset;
    this.sampleCount = sampleCount;
  }
}
