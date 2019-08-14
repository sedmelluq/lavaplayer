package com.sedmelluq.discord.lavaplayer.container.mpeg.reader;

import java.io.IOException;

/**
 * Handles one MPEG section which has no version info
 */
public interface MpegSectionHandler {
  /**
   * @param child The section
   * @throws IOException On read error
   */
  void handle(MpegSectionInfo child) throws IOException;
}
