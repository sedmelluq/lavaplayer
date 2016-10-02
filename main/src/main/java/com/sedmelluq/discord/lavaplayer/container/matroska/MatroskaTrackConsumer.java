package com.sedmelluq.discord.lavaplayer.container.matroska;

import org.ebml.matroska.MatroskaFileFrame;
import org.ebml.matroska.MatroskaFileTrack;

/**
 * Consumer for the file frames of a specific matroska file track
 */
public interface MatroskaTrackConsumer {
  /**
   * @return The associated matroska file track
   */
  MatroskaFileTrack getTrack();

  /**
   * Initialise the consumer, called before first consume()
   */
  void initialise();

  /**
   * Indicates that the next frame is not a direct continuation of the previous one
   *
   * @param requestedTimecode Timecode in milliseconds to which the seek was requested to
   * @param providedTimecode Timecode in milliseconds to which the seek was actually performed to
   */
  void seekPerformed(long requestedTimecode, long providedTimecode);

  /**
   * Indicates that no more input will come, all remaining buffers should be flushed
   * @throws InterruptedException
   */
  void flush() throws InterruptedException;

  /**
   * Consume one frame from the track
   *
   * @param frame The frame to consume
   * @throws InterruptedException
   */
  void consume(MatroskaFileFrame frame) throws InterruptedException;

  /**
   * Already flushed, no more input coming. Free all resources
   */
  void close();
}
