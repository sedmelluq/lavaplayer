package com.sedmelluq.lavaplayer.core.source.vimeo;

import com.sedmelluq.lavaplayer.core.http.HttpClientTools;
import com.sedmelluq.lavaplayer.core.http.HttpConfigurable;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.HttpInterfaceManager;
import com.sedmelluq.lavaplayer.core.info.AudioInfoEntity;
import com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.request.generic.GenericAudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoBuilder;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoTemplate;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.source.AudioSource;
import com.sedmelluq.lavaplayer.core.tools.DataFormatTools;
import com.sedmelluq.lavaplayer.core.tools.JsonBrowser;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreAuthor;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreIdentifier;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreIsStream;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreLength;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreSourceName;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreTitle;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreUrl;
import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager which detects Vimeo tracks by URL.
 */
public class VimeoAudioSource implements AudioSource, HttpConfigurable {
  private static final String NAME = "vimeo";
  private static final AudioTrackProperty sourceProperty = coreSourceName(NAME);

  private static final String TRACK_URL_REGEX = "^https://vimeo.com/[0-9]+(?:\\?.*|)$";
  private static final Pattern trackUrlPattern = Pattern.compile(TRACK_URL_REGEX);

  private final HttpInterfaceManager httpInterfaceManager;

  /**
   * Create an instance.
   */
  public VimeoAudioSource() {
    httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public AudioInfoEntity loadItem(AudioInfoRequest request) {
    if (!(request instanceof GenericAudioInfoRequest)) {
      return null;
    }

    String hint = ((GenericAudioInfoRequest) request).getHint();

    if (!trackUrlPattern.matcher(hint).matches()) {
      return null;
    }

    try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
      return loadFromTrackPage(request, httpInterface, hint);
    } catch (IOException e) {
      throw new FriendlyException("Loading Vimeo track information failed.", SUSPICIOUS, e);
    }
  }

  @Override
  public AudioTrackInfo decorateTrackInfo(AudioTrackInfo trackInfo) {
    return trackInfo;
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo) {
    return new VimeoUrlPlayback(trackInfo.getIdentifier(), this);
  }

  @Override
  public void close() {
    ExceptionTools.closeWithWarnings(httpInterfaceManager);
  }

  /**
   * @return Get an HTTP interface for a playing track.
   */
  public HttpInterface getHttpInterface() {
    return httpInterfaceManager.getInterface();
  }

  @Override
  public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
    httpInterfaceManager.configureRequests(configurator);
  }

  @Override
  public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
    httpInterfaceManager.configureBuilder(configurator);
  }

  JsonBrowser loadConfigJsonFromPageContent(String content) throws IOException {
    String configText = DataFormatTools.extractBetween(content, "window.vimeo.clip_page_config = ", "\n");

    if (configText != null) {
      return JsonBrowser.parse(configText);
    }

    return null;
  }

  private AudioInfoEntity loadFromTrackPage(
      AudioTrackInfoTemplate template,
      HttpInterface httpInterface,
      String trackUrl
  ) throws IOException {
    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(trackUrl))) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode == HttpStatus.SC_NOT_FOUND) {
        return AudioInfoEntity.NO_INFO;
      } else if (!HttpClientTools.isSuccessWithContent(statusCode)) {
        throw new FriendlyException("Server responded with an error.", SUSPICIOUS,
            new IllegalStateException("Response code is " + statusCode));
      }

      String content = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
      return loadTrackFromPageContent(template, trackUrl, content);
    }
  }

  private AudioTrackInfo loadTrackFromPageContent(
      AudioTrackInfoTemplate template,
      String trackUrl,
      String content
  ) throws IOException {
    JsonBrowser config = loadConfigJsonFromPageContent(content);

    if (config == null) {
      throw new FriendlyException("Track information not found on the page.", SUSPICIOUS, null);
    }

    return AudioTrackInfoBuilder.fromTemplate(template)
        .with(sourceProperty)
        .with(coreTitle(config.get("clip").get("title").text()))
        .with(coreAuthor(config.get("owner").get("display_name").text()))
        .with(coreLength((long) (config.get("clip").get("duration").get("raw").as(Double.class) * 1000.0)))
        .with(coreIdentifier(trackUrl))
        .with(coreIsStream(false))
        .with(coreUrl(trackUrl))
        .build();
  }
}
