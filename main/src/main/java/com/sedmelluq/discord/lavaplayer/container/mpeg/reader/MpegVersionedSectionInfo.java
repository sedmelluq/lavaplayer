package com.sedmelluq.discord.lavaplayer.container.mpeg.reader;

/**
 * Information for one MP4 section (aka box) including version and flags
 */
public class MpegVersionedSectionInfo extends MpegSectionInfo {
  /**
   * Version of the section
   */
  public final int version;
  /**
   * Flags of the section
   */
  public final int flags;

  /**
   * @param sectionInfo Basic info for the section
   * @param version Version of the section
   * @param flags Flags of the section
   */
  public MpegVersionedSectionInfo(MpegSectionInfo sectionInfo, int version, int flags) {
    super(sectionInfo.offset, sectionInfo.length, sectionInfo.type);

    this.version = version;
    this.flags = flags;
  }
}
