package com.sedmelluq.discord.lavaplayer.container.ogg;

import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.io.IOException;

/**
 * A handler for a specific codec for an OGG stream.
 */
public interface OggTrackHandler {
  /**
   * Initialises the track stream.
   *
   * @param context Configuration and output information for processing
   * @throws IOException On read error.
   */
  void initialise(AudioProcessingContext context) throws IOException;

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
