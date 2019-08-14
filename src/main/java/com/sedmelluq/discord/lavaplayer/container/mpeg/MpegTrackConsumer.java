package com.sedmelluq.discord.lavaplayer.container.mpeg;

import java.nio.channels.ReadableByteChannel;

/**
 * Consumer for the data of one MP4 track
 */
public interface MpegTrackConsumer {
  /**
   * @return The associated MP4 track
   */
  MpegTrackInfo getTrack();

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
   * Indicates that no more input is coming. Flush any buffers to output.
   * @throws InterruptedException When interrupted externally (or for seek/stop).
   */
  void flush() throws InterruptedException;

  /**
   * Consume one chunk from the track
   * @param channel Byte channel to consume from
   * @param length Lenth of the chunk in bytes
   * @throws InterruptedException When interrupted externally (or for seek/stop).
   */
  void consume(ReadableByteChannel channel, int length) throws InterruptedException;

  /**
   * Free all resources
   */
  void close();
}
