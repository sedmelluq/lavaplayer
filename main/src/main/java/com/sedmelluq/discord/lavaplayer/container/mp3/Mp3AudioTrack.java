package com.sedmelluq.discord.lavaplayer.container.mp3;

import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BaseAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audio track that handles an MP3 stream
 */
public class Mp3AudioTrack extends BaseAudioTrack {
  private static final Logger log = LoggerFactory.getLogger(Mp3AudioTrack.class);

  private final SeekableInputStream inputStream;

  /**
   * @param trackInfo Track info
   * @param inputStream Input stream for the MP3 file
   */
  public Mp3AudioTrack(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    super(trackInfo);

    this.inputStream = inputStream;
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
    Mp3TrackProvider provider = new Mp3TrackProvider(localExecutor.getProcessingContext(), inputStream);

    try {
      provider.parseHeaders();

      log.debug("Starting to play MP3 track {}", getIdentifier());
      localExecutor.executeProcessingLoop(provider::provideFrames, provider::seekToTimecode);
    } finally {
      provider.close();
    }
  }
}
