package com.sedmelluq.lavaplayer.core.container.flac;

import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audio track that handles a FLAC stream
 */
public class FlacPlayback implements AudioPlayback {
  private static final Logger log = LoggerFactory.getLogger(FlacPlayback.class);

  private final String identifier;
  private final SeekableInputStream inputStream;

  public FlacPlayback(String identifier, SeekableInputStream inputStream) {
    this.identifier = identifier;
    this.inputStream = inputStream;
  }

  @Override
  public void process(AudioPlaybackController controller) {
    FlacFileLoader file = new FlacFileLoader(inputStream);

    try (FlacTrackProvider trackProvider = file.loadTrack(controller.getContext())) {
      log.debug("Starting to play FLAC track {}", identifier);
      controller.executeProcessingLoop(trackProvider::provideFrames, trackProvider::seekToTimecode);
    } catch (IOException e) {
      throw ExceptionTools.toRuntimeException(e);
    }
  }
}
