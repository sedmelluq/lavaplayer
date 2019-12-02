package com.sedmelluq.lavaplayer.core.container.mpeg.reader;

import com.sedmelluq.lavaplayer.core.container.mpeg.reader.MpegSectionInfo;
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
