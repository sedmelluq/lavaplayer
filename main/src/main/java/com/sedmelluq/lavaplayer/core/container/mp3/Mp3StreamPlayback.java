package com.sedmelluq.lavaplayer.core.container.mp3;

import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mp3StreamPlayback implements AudioPlayback {
  private static final Logger log = LoggerFactory.getLogger(Mp3StreamPlayback.class);

  private final String identifier;
  private final SeekableInputStream inputStream;

  public Mp3StreamPlayback(String identifier, SeekableInputStream inputStream) {
    this.identifier = identifier;
    this.inputStream = inputStream;
  }

  @Override
  public void process(AudioPlaybackController controller) {
    try (Mp3TrackProvider provider = new Mp3TrackProvider(controller.getContext(), inputStream)) {
      provider.parseHeaders();

      log.debug("Starting to play MP3 track {}", identifier);
      controller.executeProcessingLoop(provider::provideFrames, provider::seekToTimecode);
    } catch (IOException e) {
      throw ExceptionTools.toRuntimeException(e);
    }
  }
}
