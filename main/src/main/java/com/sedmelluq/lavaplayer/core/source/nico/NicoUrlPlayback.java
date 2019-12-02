package com.sedmelluq.lavaplayer.core.source.nico;

import com.sedmelluq.lavaplayer.core.container.mpeg.MpegStreamPlayback;
import com.sedmelluq.lavaplayer.core.http.HttpClientTools;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.PersistentHttpStream;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.tools.DataFormatTools.convertToMapLayout;

public class NicoUrlPlayback implements AudioPlayback {
  private static final Logger log = LoggerFactory.getLogger(NicoUrlPlayback.class);

  private final String url;
  private final NicoAudioSource sourceManager;

  public NicoUrlPlayback(String url, NicoAudioSource sourceManager) {
    this.url = url;
    this.sourceManager = sourceManager;
  }

  @Override
  public void process(AudioPlaybackController controller) {
    sourceManager.checkLoggedIn();

    try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
      loadVideoMainPage(httpInterface);
      String playbackUrl = loadPlaybackUrl(httpInterface);

      log.debug("Starting NicoNico track from URL: {}", playbackUrl);

      try (PersistentHttpStream stream = new PersistentHttpStream(httpInterface, URI.create(playbackUrl), null)) {
        new MpegStreamPlayback(stream).process(controller);
      }
    } catch (IOException e) {
      throw ExceptionTools.toRuntimeException(e);
    }
  }

  private void loadVideoMainPage(HttpInterface httpInterface) throws IOException {
    HttpGet request = new HttpGet("http://www.nicovideo.jp/watch/" + url);

    try (CloseableHttpResponse response = httpInterface.execute(request)) {
      int statusCode = response.getStatusLine().getStatusCode();
      if (!HttpClientTools.isSuccessWithContent(statusCode)) {
        throw new IOException("Unexpected status code from video main page: " + statusCode);
      }

      EntityUtils.consume(response.getEntity());
    }
  }

  private String loadPlaybackUrl(HttpInterface httpInterface) throws IOException {
    HttpGet request = new HttpGet("http://flapi.nicovideo.jp/api/getflv/" + url);

    try (CloseableHttpResponse response = httpInterface.execute(request)) {
      int statusCode = response.getStatusLine().getStatusCode();
      if (!HttpClientTools.isSuccessWithContent(statusCode)) {
        throw new IOException("Unexpected status code from playback parameters page: " + statusCode);
      }

      String text = EntityUtils.toString(response.getEntity());
      Map<String, String> format = convertToMapLayout(URLEncodedUtils.parse(text, StandardCharsets.UTF_8));

      return format.get("url");
    }
  }
}
