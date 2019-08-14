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
   * Flush everything to output.
   *
   * @throws InterruptedException When interrupted externally (or for seek/stop).
   */
  void flush() throws InterruptedException;

  /**
   * Free all resources. No more input is expected.
   */
  void close();
}
