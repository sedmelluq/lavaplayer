package com.sedmelluq.lavaplayer.core.source.twitch;

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
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.source.AudioSource;
import com.sedmelluq.lavaplayer.core.tools.JsonBrowser;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
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
 * Audio source manager which detects Twitch tracks by URL.
 */
public class TwitchStreamAudioSource implements AudioSource, HttpConfigurable {
  private static final String NAME = "twitch";
  private static final AudioTrackProperty sourceProperty = coreSourceName(NAME);

  private static final String STREAM_NAME_REGEX = "^https://(?:www\\.|go\\.)?twitch.tv/([^/]+)$";
  private static final Pattern streamNameRegex = Pattern.compile(STREAM_NAME_REGEX);

  public static final String DEFAULT_CLIENT_ID = "jzkbprff40iqj646a697cyrvl0zt2m6";

  private final HttpInterfaceManager httpInterfaceManager;
  private final String twitchClientId;

  /**
   * Create an instance.
   */
  public TwitchStreamAudioSource() {
    this(DEFAULT_CLIENT_ID);
  }

  /**
   * Create an instance.
   * @param clientId The Twitch client id for your application.
   */
  public TwitchStreamAudioSource(String clientId) {
      httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
      twitchClientId = clientId;
  }

  public String getClientId() {
    return twitchClientId;
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
    String streamName = getChannelIdentifierFromUrl(hint);
    if (streamName == null) {
      return null;
    }

    JsonBrowser channelInfo = fetchStreamChannelInfo(streamName);

    if (channelInfo == null) {
      return AudioInfoEntity.NO_INFO;
    } else {
      //Use the stream name as the display name (we would require an additional call to the user to get the true display name)
      String displayName = streamName;

      //Retrieve the data value list; this will have only one element since we're getting only one stream's information
      List<JsonBrowser> dataList = channelInfo.get("data").values();

      //The value list is empty if the stream is offline, even when hosting another channel
      if (dataList.size() == 0){
        return null;
      }

      //The first one has the title of the broadcast
      JsonBrowser channelData = dataList.get(0);
      String status = channelData.get("title").text();

      return AudioTrackInfoBuilder.fromTemplate(request)
          .with(sourceProperty)
          .with(coreTitle(status))
          .with(coreAuthor(displayName))
          .with(coreLength(Long.MAX_VALUE))
          .with(coreIdentifier(hint))
          .with(coreIsStream(true))
          .with(coreUrl(hint))
          .build();
    }
  }

  @Override
  public AudioTrackInfo decorateTrackInfo(AudioTrackInfo trackInfo) {
    return trackInfo;
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo) {
    return new TwitchUrlPlayback(trackInfo.getIdentifier(), this);
  }

  @Override
  public void close() {
    ExceptionTools.closeWithWarnings(httpInterfaceManager);
  }

  /**
   * Extract channel identifier from a channel URL.
   * @param url Channel URL
   * @return Channel identifier (for API requests)
   */
  public static String getChannelIdentifierFromUrl(String url) {
    Matcher matcher = streamNameRegex.matcher(url);
    if (!matcher.matches()) {
      return null;
    }

    return matcher.group(1);
  }

  /**
   * @param url Request URL
   * @return Request with necessary headers attached.
   */
  public HttpUriRequest createGetRequest(String url) {
    return addClientHeaders(new HttpGet(url), twitchClientId);
  }

  /**
   * @param url Request URL
   * @return Request with necessary headers attached.
   */
  public HttpUriRequest createGetRequest(URI url) {
    return addClientHeaders(new HttpGet(url), twitchClientId);
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

  private static HttpUriRequest addClientHeaders(HttpUriRequest request, String clientId) {
    request.setHeader("Client-ID", clientId);
    return request;
  }

  private JsonBrowser fetchStreamChannelInfo(String name) {
    try (HttpInterface httpInterface = getHttpInterface()) {
      HttpUriRequest request = createGetRequest("https://api.twitch.tv/helix/streams?user_login=" + name);

      return HttpClientTools.fetchResponseAsJson(httpInterface, request, true);
    } catch (IOException e) {
      throw new FriendlyException("Loading Twitch channel information failed.", SUSPICIOUS, e);
    }
  }
}
