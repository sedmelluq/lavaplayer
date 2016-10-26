package com.sedmelluq.discord.lavaplayer.container.mp3;

import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;

import java.io.IOException;

/**
 * A seeking handler for MP3 files.
 */
public interface Mp3Seeker {
  /**
   * @return The duration of the file in milliseconds. May be an estimate.
   */
  long getDuration();

  /**
   * @return True if the track is seekable.
   */
  boolean isSeekable();

  /**
   * @param timecode The timecode that the seek is requested to
   * @param inputStream The input stream to perform the seek on
   * @return The index of the frame that the seek was performed to
   * @throws IOException On IO error
   */
  long seekAndGetFrameIndex(long timecode, SeekableInputStream inputStream) throws IOException;
}
