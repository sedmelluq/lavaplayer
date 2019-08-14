package com.sedmelluq.discord.lavaplayer.container.mpeg.reader;

import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegTrackConsumer;

/**
 * Track provider for a type of MP4 file.
 */
public interface MpegFileTrackProvider {
  /**
   * @param trackConsumer Track consumer which defines the track this will provide and the consumer for packets.
   * @return Returns true if it had enough information for initialisation.
   */
  boolean initialise(MpegTrackConsumer trackConsumer);

  /**
   * @return Total duration of the file in milliseconds
   */
  long getDuration();

  /**
   * Provide audio frames to the frame consumer until the end of the track or interruption.
   *
   * @throws InterruptedException When interrupted externally (or for seek/stop).
   */
  void provideFrames() throws InterruptedException;

  /**
   * Perform a seek to the given timecode (ms). On the next call to provideFrames, the seekPerformed method of frame
   * consumer is called with the position where it actually seeked to and the position where the seek was requested to
   * as arguments.
   *
   * @param timecode The timecode to seek to in milliseconds
   */
  void seekToTimecode(long timecode);
}
