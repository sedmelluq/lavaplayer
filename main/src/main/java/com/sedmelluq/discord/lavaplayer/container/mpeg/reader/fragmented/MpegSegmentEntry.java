package com.sedmelluq.discord.lavaplayer.container.mpeg.reader.fragmented;

/**
 * Information about one MP4 segment aka fragment
 */
public class MpegSegmentEntry {
  /**
   * Type of the segment
   */
  public final int type;
  /**
   * Size in bytes
   */
  public final int size;
  /**
   * Duration using the timescale of the file
   */
  public final int duration;

  /**
   * @param type Type of the segment
   * @param size Size in bytes
   * @param duration Duration using the timescale of the file
   */
  public MpegSegmentEntry(int type, int size, int duration) {
    this.type = type;
    this.size = size;
    this.duration = duration;
  }
}
