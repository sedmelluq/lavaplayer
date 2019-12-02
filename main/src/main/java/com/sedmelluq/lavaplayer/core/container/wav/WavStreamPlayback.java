package com.sedmelluq.lavaplayer.core.container.wav;

import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WavStreamPlayback implements AudioPlayback {
  private static final Logger log = LoggerFactory.getLogger(WavStreamPlayback.class);

  private final String identifier;
  private final SeekableInputStream inputStream;

  public WavStreamPlayback(String identifier, SeekableInputStream inputStream) {
    this.identifier = identifier;
    this.inputStream = inputStream;
  }

  @Override
  public void process(AudioPlaybackController controller) {
    WavFileLoader loader = new WavFileLoader(inputStream);

    try (WavTrackProvider trackProvider = loader.loadTrack(controller.getContext())) {
      log.debug("Starting to play WAV track {}", identifier);
      controller.executeProcessingLoop(trackProvider::provideFrames, trackProvider::seekToTimecode);
    } catch (IOException e) {
      throw ExceptionTools.toRuntimeException(e);
    }
  }
}
