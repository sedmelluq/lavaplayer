package com.sedmelluq.lavaplayer.core.source.nico;

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
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreAuthor;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreIdentifier;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreIsStream;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreLength;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreSourceName;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreTitle;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreUrl;
import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.COMMON;
import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager that implements finding NicoNico tracks based on URL.
 */
public class NicoAudioSource implements AudioSource, HttpConfigurable {
  private static final String NAME = "niconico";
  private static final AudioTrackProperty sourceProperty = coreSourceName(NAME);

  private static final String TRACK_URL_REGEX = "^(?:http://|https://|)(?:www\\.|)nicovideo\\.jp/watch/(sm[0-9]+)(?:\\?.*|)$";

  private static final Pattern trackUrlPattern = Pattern.compile(TRACK_URL_REGEX);

  private final String email;
  private final String password;
  private final HttpInterfaceManager httpInterfaceManager;
  private final AtomicBoolean loggedIn;

  /**
   * @param email Site account email
   * @param password Site account password
   */
  public NicoAudioSource(String email, String password) {
    this.email = email;
    this.password = password;
    httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
    loggedIn = new AtomicBoolean();
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public AudioInfoEntity loadItem(AudioInfoRequest request) {
    if (request instanceof GenericAudioInfoRequest) {
      String hint = ((GenericAudioInfoRequest) request).getHint();
      Matcher trackMatcher = trackUrlPattern.matcher(hint);

      if (trackMatcher.matches()) {
        return loadTrack(request, trackMatcher.group(1));
      }
    }

    return null;
  }

  @Override
  public AudioTrackInfo decorateTrackInfo(AudioTrackInfo trackInfo) {
    return trackInfo;
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo) {
    return new NicoUrlPlayback(trackInfo.getIdentifier(), this);
  }

  @Override
  public void close() {
    ExceptionTools.closeWithWarnings(httpInterfaceManager);
  }

  private AudioTrackInfo loadTrack(AudioTrackInfoTemplate template, String videoId) {
    checkLoggedIn();

    try (HttpInterface httpInterface = getHttpInterface()) {
      try (CloseableHttpResponse response = httpInterface.execute(new HttpGet("http://ext.nicovideo.jp/api/getthumbinfo/" + videoId))) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (!HttpClientTools.isSuccessWithContent(statusCode)) {
          throw new IOException("Unexpected response code from video info: " + statusCode);
        }

        Document document = Jsoup.parse(response.getEntity().getContent(), StandardCharsets.UTF_8.name(), "", Parser.xmlParser());
        return extractTrackFromXml(template, videoId, document);
      }
    } catch (IOException e) {
      throw new FriendlyException("Error occurred when extracting video info.", SUSPICIOUS, e);
    }
  }

  private AudioTrackInfo extractTrackFromXml(AudioTrackInfoTemplate template, String videoId, Document document) {
    for (Element element : document.select(":root > thumb")) {
      String uploader = element.select("user_nickname").first().text();
      String title = element.select("title").first().text();
      long duration = DataFormatTools.durationTextToMillis(element.select("length").first().text());

      return AudioTrackInfoBuilder.fromTemplate(template)
          .with(sourceProperty)
          .with(coreTitle(title))
          .with(coreAuthor(uploader))
          .with(coreLength(duration))
          .with(coreIdentifier(videoId))
          .with(coreIsStream(false))
          .with(coreUrl(getWatchUrl(videoId)))
          .build();
    }

    return null;
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

  void checkLoggedIn() {
    synchronized (loggedIn) {
      if (loggedIn.get()) {
        return;
      }

      HttpPost loginRequest = new HttpPost("https://secure.nicovideo.jp/secure/login");

      loginRequest.setEntity(new UrlEncodedFormEntity(Arrays.asList(
          new BasicNameValuePair("mail", email),
          new BasicNameValuePair("password", password)
      ), StandardCharsets.UTF_8));

      try (HttpInterface httpInterface = getHttpInterface()) {
        try (CloseableHttpResponse response = httpInterface.execute(loginRequest)) {
          int statusCode = response.getStatusLine().getStatusCode();

          if (statusCode != 302) {
            throw new IOException("Unexpected response code " + statusCode);
          }

          Header location = response.getFirstHeader("Location");

          if (location == null || location.getValue().contains("message=")) {
            throw new FriendlyException("Login details for NicoNico are invalid.", COMMON, null);
          }

          loggedIn.set(true);
        }
      } catch (IOException e) {
        throw new FriendlyException("Exception when trying to log into NicoNico", SUSPICIOUS, e);
      }
    }
  }

  private static String getWatchUrl(String videoId) {
    return "http://www.nicovideo.jp/watch/" + videoId;
  }
}
