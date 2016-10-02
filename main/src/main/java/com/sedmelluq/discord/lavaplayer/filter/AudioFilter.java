package com.sedmelluq.discord.lavaplayer.filter;

/**
 * A filter for audio samples
 */
public interface AudioFilter {
  /**
   * Indicates that the next samples are not a continuation from the previous ones and gives the timecode for the
   * next incoming sample.
   *
   * @param requestedTime Timecode in milliseconds to which the seek was requested to
   * @param providedTime Timecode in milliseconds to which the seek was actually performed to
   */
  void seekPerformed(long requestedTime, long providedTime);

  /**
   * Flush everything to output. This indicates the end of the track so it is okay to pad the output with silence if
   * necessary to achieve a fixed packet size.
   *
   * @throws InterruptedException When interrupted
   */
  void flush() throws InterruptedException;

  /**
   * Free all resources. No more input is expected.
   */
  void close();
}
