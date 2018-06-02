package com.sedmelluq.discord.lavaplayer.tools;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Contains constants with metadata about the library.
 */
public class PlayerLibrary {
  /**
   * The currently loaded version of the library.
   */
  public static final String VERSION = readVersion();

  private static String readVersion() {
    InputStream stream = PlayerLibrary.class.getResourceAsStream("version.txt");

    try {
      if (stream != null) {
        return IOUtils.toString(stream, StandardCharsets.UTF_8);
      }
    } catch (Exception e) {
      // Something went wrong.
    }

    return "UNKNOWN";
  }
}
