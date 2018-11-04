package com.sedmelluq.discord.lavaplayer.container.ogg;

import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.io.IOException;

/**
 * A handler for a specific codec for an OGG stream.
 */
public interface OggTrackProvider {
  /**
   * Initialises the track stream.
   *
   * @param context Configuration and output information for processing
   * @throws IOException On read error.
   */
  void initialise(AudioProcessingContext context) throws IOException;

  /**
   * @return Tags loaded from the track. If tags were not parsed or format does not support it, empty map is returned.
   */
  OggMetadata getMetadata();

  /**
   * @return If the track header contains stream size information, returns that. Otherwise if the stream size
   *         information can be determined by seeking in the stream (stream must be seekable and have a known length),
   *         will seek to determine the size info.
   * @throws IOException On read error.
   */
  OggStreamSizeInfo seekForSizeInfo() throws IOException;

  /**
   * Decodes audio frames and sends them to frame consumer.
   *
   * @throws InterruptedException When interrupted externally (or for seek/stop).
   */
  void provideFrames() throws InterruptedException;

  /**
   * Seeks to the specified timecode.
   *
   * @param timecode The timecode in milliseconds
   */
  void seekToTimecode(long timecode);

  /**
   * Free all resources associated to processing the track.
   */
  void close();
}
