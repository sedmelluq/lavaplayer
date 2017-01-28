package com.sedmelluq.discord.lavaplayer.container.wav;

import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BaseAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audio track that handles a WAV stream
 */
public class WavAudioTrack extends BaseAudioTrack {
  private static final Logger log = LoggerFactory.getLogger(WavAudioTrack.class);

  private final SeekableInputStream inputStream;

  /**
   * @param trackInfo Track info
   * @param inputStream Input stream for the WAV file
   */
  public WavAudioTrack(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    super(trackInfo);

    this.inputStream = inputStream;
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
    WavTrackProvider trackProvider = new WavFileLoader(inputStream).loadTrack(localExecutor.getProcessingContext());

    try {
      log.debug("Starting to play WAV track {}", getIdentifier());
      localExecutor.executeProcessingLoop(trackProvider::provideFrames, trackProvider::seekToTimecode);
    } finally {
      trackProvider.close();
    }
  }
}
