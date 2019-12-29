package com.sedmelluq.lavaplayer.extensions.format.xm;

import com.sedmelluq.discord.lavaplayer.container.wav.WavAudioTrack;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BaseAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmAudioTrack extends BaseAudioTrack {
  private static final Logger log = LoggerFactory.getLogger(WavAudioTrack.class);

  private final SeekableInputStream inputStream;

  /**
   * @param trackInfo   Track info
   * @param inputStream Input stream for the WAV file
   */
  public XmAudioTrack(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    super(trackInfo);

    this.inputStream = inputStream;
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
    XmTrackProvider trackProvider = new XmFileLoader(inputStream).loadTrack(localExecutor.getProcessingContext());

    try {
      log.debug("Starting to play module {}", getIdentifier());
      localExecutor.executeProcessingLoop(trackProvider::provideFrames, null);
    } finally {
      trackProvider.close();
    }
  }
}
