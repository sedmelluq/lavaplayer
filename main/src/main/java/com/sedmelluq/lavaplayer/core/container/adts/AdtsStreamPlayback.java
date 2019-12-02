package com.sedmelluq.lavaplayer.core.container.adts;

import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audio track that handles an ADTS packet stream
 */
public class AdtsStreamPlayback implements AudioPlayback {
  private static final Logger log = LoggerFactory.getLogger(AdtsStreamPlayback.class);

  private final String identifier;
  private final InputStream inputStream;

  public AdtsStreamPlayback(String identifier, InputStream inputStream) {
    this.identifier = identifier;
    this.inputStream = inputStream;
  }

  @Override
  public void process(AudioPlaybackController controller) {
    try (AdtsStreamProvider provider = new AdtsStreamProvider(inputStream, controller.getContext())) {
      log.debug("Starting to play ADTS stream {}", identifier);

      controller.executeProcessingLoop(provider::provideFrames, null);
    }
  }
}
