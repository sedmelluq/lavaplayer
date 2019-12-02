package com.sedmelluq.lavaplayer.core.source.beam;

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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.client.config.RequestConfig;
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
 * Audio source manager which detects Beam.pro tracks by URL.
 */
public class BeamAudioSource implements AudioSource, HttpConfigurable {
  private static final String NAME = "beam.pro";
  private static final AudioTrackProperty sourceProperty = coreSourceName(NAME);

  private static final String STREAM_NAME_REGEX = "^https://(?:www\\.)?(?:beam\\.pro|mixer\\.com)/([^/]+)$";
  private static final Pattern streamNameRegex = Pattern.compile(STREAM_NAME_REGEX);

  private final HttpInterfaceManager httpInterfaceManager;

  /**
   * Create an instance.
   */
  public BeamAudioSource() {
    this.httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
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
    String streamName = getChannelNameFromUrl(hint);
    if (streamName == null) {
      return null;
    }

    JsonBrowser channelInfo = fetchStreamChannelInfo(streamName);

    if (channelInfo == null) {
      return AudioInfoEntity.NO_INFO;
    } else {
      String displayName = channelInfo.get("name").text();
      String id = getPlayedStreamId(channelInfo);

      if (displayName == null || id == null) {
        throw new IllegalStateException("Expected id and name fields from Beam channel info.");
      }

      return AudioTrackInfoBuilder.fromTemplate(request)
          .with(sourceProperty)
          .with(coreTitle(displayName))
          .with(coreAuthor(streamName))
          .with(coreLength(Long.MAX_VALUE))
          .with(coreIdentifier(id + "|" + streamName + "|" + hint))
          .with(coreIsStream(true))
          .with(coreUrl("https://beam.pro/" + streamName))
          .build();
    }
  }

  @Override
  public AudioTrackInfo decorateTrackInfo(AudioTrackInfo trackInfo) {
    return trackInfo;
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo) {
    return new BeamUrlPlayback(trackInfo.getIdentifier(), httpInterfaceManager);
  }

  @Override
  public void close() {
    ExceptionTools.closeWithWarnings(httpInterfaceManager);
  }

  private static String getPlayedStreamId(JsonBrowser channelInfo) {
    // If there is a hostee, this means that the current channel itself is not actually broadcasting anything and all
    // further requests should be performed with the ID of the hostee. Hostee is not rechecked later so it will keep
    // playing the current hostee even if it changes.
    String hosteeId = channelInfo.get("hosteeId").text();
    return hosteeId != null ? hosteeId : channelInfo.get("id").text();
  }

  private static String getChannelNameFromUrl(String url) {
    Matcher matcher = streamNameRegex.matcher(url);
    if (!matcher.matches()) {
      return null;
    }

    return matcher.group(1);
  }

  private JsonBrowser fetchStreamChannelInfo(String name) {
    try (HttpInterface httpInterface = getHttpInterface()) {
      return HttpClientTools.fetchResponseAsJson(httpInterface,
          new HttpGet("https://mixer.com/api/v1/channels/" + name + "?noCount=1"));
    } catch (IOException e) {
      throw new FriendlyException("Loading Beam channel information failed.", SUSPICIOUS, e);
    }
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
}
