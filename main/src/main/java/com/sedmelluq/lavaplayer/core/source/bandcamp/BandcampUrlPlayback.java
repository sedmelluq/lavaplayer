package com.sedmelluq.lavaplayer.core.source.bandcamp;

import com.sedmelluq.lavaplayer.core.container.mp3.Mp3StreamPlayback;
import com.sedmelluq.lavaplayer.core.http.HttpClientTools;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.PersistentHttpStream;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import com.sedmelluq.lavaplayer.core.tools.JsonBrowser;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BandcampUrlPlayback implements AudioPlayback {
  private static final Logger log = LoggerFactory.getLogger(BandcampUrlPlayback.class);

  private final String url;
  private final BandcampAudioSource sourceManager;

  public BandcampUrlPlayback(String url, BandcampAudioSource sourceManager) {
    this.url = url;
    this.sourceManager = sourceManager;
  }

  @Override
  public void process(AudioPlaybackController controller) {
    try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
      log.debug("Loading Bandcamp track page from URL: {}", url);

      String trackMediaUrl = getTrackMediaUrl(httpInterface);
      log.debug("Starting Bandcamp track from URL: {}", trackMediaUrl);

      try (PersistentHttpStream stream = new PersistentHttpStream(
          httpInterface, URI.create(trackMediaUrl), null)) {

        new Mp3StreamPlayback(url, stream).process(controller);
      }
    } catch (IOException e) {
      throw ExceptionTools.toRuntimeException(e);
    }
  }

  private String getTrackMediaUrl(HttpInterface httpInterface) throws IOException {
    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(url))) {
      int statusCode = response.getStatusLine().getStatusCode();
      if (!HttpClientTools.isSuccessWithContent(statusCode)) {
        throw new IOException("Invalid status code for track page: " + statusCode);
      }

      String responseText = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
      JsonBrowser trackInfo = sourceManager.readTrackListInformation(responseText);

      return trackInfo.get("trackinfo").index(0).get("file").get("mp3-128").text();
    }
  }
}
