package com.sedmelluq.discord.lavaplayer.container.mpeg.reader;

import java.io.IOException;

/**
 * Handles one MPEG section which has version info
 */
public interface MpegVersionedSectionHandler {
  /**
   * @param child The versioned section
   * @throws IOException On read error
   */
  void handle(MpegVersionedSectionInfo child) throws IOException;
}
