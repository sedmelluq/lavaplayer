package com.sedmelluq.discord.lavaplayer.source.vimeo;

import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpAccessPoint;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio track that handles processing Vimeo tracks.
 */
public class VimeoAudioTrack extends DelegatedAudioTrack {
  private static final Logger log = LoggerFactory.getLogger(VimeoAudioTrack.class);

  private final VimeoAudioSourceManager sourceManager;

  /**
   * @param trackInfo Track info
   * @param sourceManager Source manager which was used to find this track
   */
  public VimeoAudioTrack(AudioTrackInfo trackInfo, VimeoAudioSourceManager sourceManager) {
    super(trackInfo);

    this.sourceManager = sourceManager;
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
    try (HttpAccessPoint accessPoint = sourceManager.getAccessPoint()) {
      String playbackUrl = loadPlaybackUrl(accessPoint);

      log.debug("Starting Vimeo track from URL: {}", playbackUrl);

      try (PersistentHttpStream stream = new PersistentHttpStream(accessPoint, new URI(playbackUrl), null)) {
        processDelegate(new MpegAudioTrack(trackInfo, stream), localExecutor);
      }
    }
  }

  private String loadPlaybackUrl(HttpAccessPoint accessPoint) throws IOException {
    JsonBrowser config = loadPlayerConfig(accessPoint);
    if (config == null) {
      throw new FriendlyException("Track information not present on the page.", SUSPICIOUS, null);
    }

    String trackConfigUrl = config.get("player").get("config_url").text();
    JsonBrowser trackConfig = loadTrackConfig(accessPoint, trackConfigUrl);

    return trackConfig.get("request").get("files").get("progressive").index(0).get("url").text();
  }

  private JsonBrowser loadPlayerConfig(HttpAccessPoint accessPoint) throws IOException {
    try (CloseableHttpResponse response = accessPoint.execute(new HttpGet(trackInfo.identifier))) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode != 200) {
        throw new FriendlyException("Server responded with an error.", SUSPICIOUS,
            new IllegalStateException("Response code for player config is " + statusCode));
      }

      return sourceManager.loadConfigJsonFromPageContent(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
    }
  }

  private JsonBrowser loadTrackConfig(HttpAccessPoint accessPoint, String trackAccessInfoUrl) throws IOException {
    try (CloseableHttpResponse response = accessPoint.execute(new HttpGet(trackAccessInfoUrl))) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode != 200) {
        throw new FriendlyException("Server responded with an error.", SUSPICIOUS,
            new IllegalStateException("Response code for track access info is " + statusCode));
      }

      return JsonBrowser.parse(response.getEntity().getContent());
    }
  }

  @Override
  public AudioTrack makeClone() {
    return new VimeoAudioTrack(trackInfo, sourceManager);
  }

  @Override
  public AudioSourceManager getSourceManager() {
    return sourceManager;
  }
}
