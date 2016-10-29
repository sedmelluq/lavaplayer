package com.sedmelluq.discord.lavaplayer.source.bandcamp;

import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Audio track that handles processing Bandcamp tracks.
 */
public class BandcampAudioTrack extends DelegatedAudioTrack {
  private static final Logger log = LoggerFactory.getLogger(BandcampAudioTrack.class);

  private final BandcampAudioSourceManager sourceManager;

  /**
   * @param trackInfo Track info
   * @param sourceManager Source manager which was used to find this track
   */
  public BandcampAudioTrack(AudioTrackInfo trackInfo, BandcampAudioSourceManager sourceManager) {
    super(trackInfo);

    this.sourceManager = sourceManager;
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
    try (CloseableHttpClient httpClient = sourceManager.createHttpClient()) {
      log.debug("Loading Bandcamp track page from URL: {}", trackInfo.identifier);

      String trackMediaUrl = getTrackMediaUrl(httpClient);
      log.debug("Starting Bandcamp track from URL: {}", trackMediaUrl);

      try (PersistentHttpStream stream = new PersistentHttpStream(httpClient, new URI(trackMediaUrl), null)) {
        processDelegate(new Mp3AudioTrack(trackInfo, stream), localExecutor);
      }
    }
  }

  private String getTrackMediaUrl(CloseableHttpClient httpClient) throws IOException {
    try (CloseableHttpResponse response = httpClient.execute(new HttpGet(trackInfo.identifier))) {
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != 200) {
        throw new IOException("Invalid status code " + statusCode + " for track page.");
      }

      String responseText = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
      JsonBrowser trackInfo = sourceManager.readTrackListInformation(responseText);

      return "https:" + trackInfo.get("trackinfo").index(0).get("file").get("mp3-128").text();
    }
  }

  @Override
  public AudioTrack makeClone() {
    return new BandcampAudioTrack(trackInfo, sourceManager);
  }

  @Override
  public AudioSourceManager getSourceManager() {
    return sourceManager;
  }
}
