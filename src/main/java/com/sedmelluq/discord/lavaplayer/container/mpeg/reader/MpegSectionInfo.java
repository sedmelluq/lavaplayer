package com.sedmelluq.discord.lavaplayer.container.mpeg.reader;

/**
 * Information for one MP4 section (aka box)
 */
public class MpegSectionInfo {
  /**
   * Absolute offset of the section
   */
  public final long offset;
  /**
   * Length of the section
   */
  public final long length;
  /**
   * Type (fourCC) of the section
   */
  public final String type;

  /**
   * @param offset Absolute offset of the section
   * @param length Length of the section
   * @param type Type (fourCC) of the section
   */
  public MpegSectionInfo(long offset, long length, String type) {
    this.offset = offset;
    this.length = length;
    this.type = type;
  }
}
