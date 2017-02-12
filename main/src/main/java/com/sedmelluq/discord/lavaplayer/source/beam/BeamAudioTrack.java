package com.sedmelluq.discord.lavaplayer.source.beam;

import com.sedmelluq.discord.lavaplayer.container.adts.AdtsAudioTrack;
import com.sedmelluq.discord.lavaplayer.container.mpegts.MpegTsElementaryInputStream;
import com.sedmelluq.discord.lavaplayer.container.mpegts.PesPacketInputStream;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.ChainedInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.discord.lavaplayer.container.mpegts.MpegTsElementaryInputStream.ADTS_ELEMENTARY_STREAM;

/**
 * Audio track that handles processing Beam.pro tracks.
 */
public class BeamAudioTrack extends DelegatedAudioTrack {
  private static final Logger log = LoggerFactory.getLogger(BeamAudioTrack.class);

  private final BeamAudioSourceManager sourceManager;
  private final BeamSegmentUrlProvider segmentUrlProvider;

  /**
   * @param trackInfo Track info
   * @param sourceManager Source manager which was used to find this track
   */
  public BeamAudioTrack(AudioTrackInfo trackInfo, BeamAudioSourceManager sourceManager) {
    super(trackInfo);

    this.sourceManager = sourceManager;
    this.segmentUrlProvider = new BeamSegmentUrlProvider(getChannelId());
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
    log.debug("Starting to play Beam channel {}.", getChannelUrl());

    try (final HttpInterface httpInterface = sourceManager.getHttpInterface()) {
      try (ChainedInputStream chainedInputStream = new ChainedInputStream(() -> segmentUrlProvider.getNextSegmentStream(httpInterface))) {
        MpegTsElementaryInputStream elementaryInputStream = new MpegTsElementaryInputStream(chainedInputStream, ADTS_ELEMENTARY_STREAM);
        PesPacketInputStream pesPacketInputStream = new PesPacketInputStream(elementaryInputStream);

        processDelegate(new AdtsAudioTrack(trackInfo, pesPacketInputStream), localExecutor);
      }
    }
  }

  @Override
  public AudioTrack makeClone() {
    return new BeamAudioTrack(trackInfo, sourceManager);
  }

  @Override
  public AudioSourceManager getSourceManager() {
    return sourceManager;
  }

  private String getChannelId() {
    return trackInfo.identifier.substring(0, trackInfo.identifier.indexOf('|'));
  }

  private String getChannelUrl() {
    return trackInfo.identifier.substring(trackInfo.identifier.lastIndexOf('|') + 1);
  }
}
