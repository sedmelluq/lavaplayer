package com.sedmelluq.discord.lavaplayer.source.twitch;

import com.sedmelluq.discord.lavaplayer.container.adts.AdtsAudioTrack;
import com.sedmelluq.discord.lavaplayer.container.mpegts.MpegTsElementaryInputStream;
import com.sedmelluq.discord.lavaplayer.container.mpegts.PesPacketInputStream;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.ChainedInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpAccessPoint;
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
public class TwitchStreamAudioTrack extends DelegatedAudioTrack {
  private static final Logger log = LoggerFactory.getLogger(TwitchStreamAudioTrack.class);

  private final TwitchStreamAudioSourceManager sourceManager;
  private final String channelName;
  private final TwitchStreamSegmentUrlProvider segmentUrlProvider;

  /**
   * @param trackInfo Track info
   * @param sourceManager Source manager which was used to find this track
   */
  public TwitchStreamAudioTrack(AudioTrackInfo trackInfo, TwitchStreamAudioSourceManager sourceManager) {
    super(trackInfo);

    this.sourceManager = sourceManager;
    this.channelName = TwitchStreamAudioSourceManager.getChannelIdentifierFromUrl(trackInfo.identifier);
    this.segmentUrlProvider = new TwitchStreamSegmentUrlProvider(channelName);
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
    log.debug("Starting to play Twitch channel {}.", channelName);

    try (final HttpAccessPoint accessPoint = sourceManager.getAccessPoint()) {
      try (ChainedInputStream chainedInputStream = new ChainedInputStream(() -> getSegmentInputStream(accessPoint))) {
        MpegTsElementaryInputStream elementaryInputStream = new MpegTsElementaryInputStream(chainedInputStream, ADTS_ELEMENTARY_STREAM);
        PesPacketInputStream pesPacketInputStream = new PesPacketInputStream(elementaryInputStream);

        processDelegate(new AdtsAudioTrack(trackInfo, pesPacketInputStream), localExecutor);
      }
    }
  }

  private InputStream getSegmentInputStream(HttpAccessPoint accessPoint) {
    String url = segmentUrlProvider.getNextSegmentUrl(accessPoint);
    if (url == null) {
      return null;
    }

    CloseableHttpResponse response = null;
    boolean success = false;

    try {
      response = accessPoint.execute(createGetRequest(url));
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode != 200) {
        throw new IOException("Invalid status code " + statusCode + " from segment data URL.");
      }

      success = true;
      return response.getEntity().getContent();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (response != null && !success) {
        IOUtils.closeQuietly(response);
      }
    }
  }

  @Override
  public AudioTrack makeClone() {
    return new TwitchStreamAudioTrack(trackInfo, sourceManager);
  }

  @Override
  public AudioSourceManager getSourceManager() {
    return sourceManager;
  }
}
