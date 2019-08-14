package com.sedmelluq.discord.lavaplayer.container.flac;

import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BaseAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audio track that handles a FLAC stream
 */
public class FlacAudioTrack extends BaseAudioTrack {
  private static final Logger log = LoggerFactory.getLogger(FlacAudioTrack.class);

  private final SeekableInputStream inputStream;

  /**
   * @param trackInfo Track info
   * @param inputStream Input stream for the FLAC file
   */
  public FlacAudioTrack(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    super(trackInfo);

    this.inputStream = inputStream;
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
    FlacFileLoader file = new FlacFileLoader(inputStream);
    FlacTrackProvider trackProvider = file.loadTrack(localExecutor.getProcessingContext());

    try {
      log.debug("Starting to play FLAC track {}", getIdentifier());
      localExecutor.executeProcessingLoop(trackProvider::provideFrames, trackProvider::seekToTimecode);
    } finally {
      trackProvider.close();
    }
  }
}
