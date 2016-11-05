package com.sedmelluq.discord.lavaplayer.container.mpeg.reader.fragmented;

/**
 * Describes the seek info for a fragmented MP4 file
 */
public class MpegGlobalSeekInfo {
  /**
   * The value of the internal timecodes that corresponds to one second
   */
  public final int timescale;
  /**
   * Size and duration information for each segment
   */
  public final MpegSegmentEntry[] entries;
  /**
   * Absolute timecode offset of each segment
   */
  public final long[] timeOffsets;
  /**
   * Absolute file offset of each segment
   */
  public final long[] fileOffsets;

  /**
   * @param timescale The value of the internal timecodes that corresponds to one second
   * @param baseOffset The file offset of the first segment
   * @param entries Size and duration information for each segment
   */
  public MpegGlobalSeekInfo(int timescale, long baseOffset, MpegSegmentEntry[] entries) {
    this.timescale = timescale;
    this.entries = entries;

    timeOffsets = new long[entries.length];
    fileOffsets = new long[entries.length];
    fileOffsets[0] = baseOffset;

    for (int i = 1; i < entries.length; i++) {
      timeOffsets[i] = timeOffsets[i-1] + entries[i-1].duration;
      fileOffsets[i] = fileOffsets[i-1] + entries[i-1].size;
    }
  }
}
