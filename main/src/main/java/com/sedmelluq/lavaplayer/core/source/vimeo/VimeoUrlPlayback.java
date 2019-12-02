package com.sedmelluq.lavaplayer.core.source.vimeo;

import com.sedmelluq.lavaplayer.core.container.mpeg.MpegStreamPlayback;
import com.sedmelluq.lavaplayer.core.http.HttpClientTools;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.PersistentHttpStream;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import com.sedmelluq.lavaplayer.core.tools.JsonBrowser;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.SUSPICIOUS;

public class VimeoUrlPlayback implements AudioPlayback {
  private static final Logger log = LoggerFactory.getLogger(VimeoUrlPlayback.class);

  private final String url;
  private final VimeoAudioSource sourceManager;

  public VimeoUrlPlayback(String url, VimeoAudioSource sourceManager) {
    this.url = url;
    this.sourceManager = sourceManager;
  }

  @Override
  public void process(AudioPlaybackController controller) {
    try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
      String playbackUrl = loadPlaybackUrl(httpInterface);

      log.debug("Starting Vimeo track from URL: {}", playbackUrl);

      try (PersistentHttpStream stream = new PersistentHttpStream(httpInterface, URI.create(playbackUrl), null)) {
        new MpegStreamPlayback(stream).process(controller);
      }
    } catch (IOException e) {
      throw ExceptionTools.toRuntimeException(e);
    }
  }

  private String loadPlaybackUrl(HttpInterface httpInterface) throws IOException {
    JsonBrowser config = loadPlayerConfig(httpInterface);
    if (config == null) {
      throw new FriendlyException("Track information not present on the page.", SUSPICIOUS, null);
    }

    String trackConfigUrl = config.get("player").get("config_url").text();
    JsonBrowser trackConfig = loadTrackConfig(httpInterface, trackConfigUrl);

    return trackConfig.get("request").get("files").get("progressive").index(0).get("url").text();
  }

  private JsonBrowser loadPlayerConfig(HttpInterface httpInterface) throws IOException {
    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(url))) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (!HttpClientTools.isSuccessWithContent(statusCode)) {
        throw new FriendlyException("Server responded with an error.", SUSPICIOUS,
            new IllegalStateException("Response code for player config is " + statusCode));
      }

      return sourceManager.loadConfigJsonFromPageContent(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
    }
  }

  private JsonBrowser loadTrackConfig(HttpInterface httpInterface, String trackAccessInfoUrl) throws IOException {
    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(trackAccessInfoUrl))) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (!HttpClientTools.isSuccessWithContent(statusCode)) {
        throw new FriendlyException("Server responded with an error.", SUSPICIOUS,
            new IllegalStateException("Response code for track access info is " + statusCode));
      }

      return JsonBrowser.parse(response.getEntity().getContent());
    }
  }
}
