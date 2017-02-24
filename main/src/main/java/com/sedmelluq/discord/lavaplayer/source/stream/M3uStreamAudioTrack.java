package com.sedmelluq.discord.lavaplayer.source.stream;

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
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import static com.sedmelluq.discord.lavaplayer.container.mpegts.MpegTsElementaryInputStream.ADTS_ELEMENTARY_STREAM;
import static com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager.createGetRequest;

/**
 * Audio track that handles processing Twitch tracks.
 */
public abstract class M3uStreamAudioTrack extends DelegatedAudioTrack {
  private final M3uStreamSegmentUrlProvider segmentUrlProvider;

  /**
   * @param trackInfo Track info
   */
  public M3uStreamAudioTrack(AudioTrackInfo trackInfo) {
    super(trackInfo);

    this.segmentUrlProvider = createSegmentProvider();
  }

  protected abstract M3uStreamSegmentUrlProvider createSegmentProvider();

  protected abstract HttpInterface getHttpInterface();

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
    try (final HttpInterface httpInterface = getHttpInterface()) {
      try (ChainedInputStream chainedInputStream = new ChainedInputStream(() -> segmentUrlProvider.getNextSegmentStream(httpInterface))) {
        MpegTsElementaryInputStream elementaryInputStream = new MpegTsElementaryInputStream(chainedInputStream, ADTS_ELEMENTARY_STREAM);
        PesPacketInputStream pesPacketInputStream = new PesPacketInputStream(elementaryInputStream);

        processDelegate(new AdtsAudioTrack(trackInfo, pesPacketInputStream), localExecutor);
      }
    }
  }
}
