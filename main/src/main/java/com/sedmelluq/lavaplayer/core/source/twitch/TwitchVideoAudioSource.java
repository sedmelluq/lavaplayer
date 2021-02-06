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
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoTemplate;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.source.AudioSource;
import com.sedmelluq.lavaplayer.core.tools.JsonBrowser;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty.Flag.PLAYBACK_CACHE;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreAuthor;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreIdentifier;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreIsStream;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreLength;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreSourceName;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreTitle;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.custom;

public class TwitchVideoAudioSource implements AudioSource, HttpConfigurable {
  private static final String PROPERTY_TIME = "cachedTokenTime";
  private static final String PROPERTY_TOKEN = "cachedToken";
  private static final String PROPERTY_SIGNATURE = "cachedTokenSignature";
  private static final long CACHE_DURATION = TimeUnit.MINUTES.toMillis(10);
  private static final AudioTrackProperty sourceProperty = coreSourceName("twitch_vod");
  private static final Pattern urlPattern = Pattern.compile("^https://(?:www\\.|go\\.)?twitch.tv/videos/([0-9]+).*$");
  private static final ContentType plainContentType = ContentType.create("text/plain", StandardCharsets.UTF_8);

  private final HttpInterfaceManager httpInterfaceManager;
  private final TwitchVideoSessionManager sessionManager;

  public TwitchVideoAudioSource() {
    this.httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
    this.sessionManager = new TwitchVideoSessionManager(httpInterfaceManager);
  }

  @Override
  public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
    httpInterfaceManager.configureRequests(configurator);
  }

  @Override
  public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
    httpInterfaceManager.configureBuilder(configurator);
  }

  @Override
  public String getName() {
    return sourceProperty.stringValue();
  }

  @Override
  public AudioInfoEntity loadItem(AudioInfoRequest request) {
    if (!(request instanceof GenericAudioInfoRequest)) {
      return null;
    }

    String hint = ((GenericAudioInfoRequest) request).getHint();
    String videoId = extractVideoIdFromUrl(hint);

    return videoId != null ? loadVideo(videoId, request) : null;
  }

  @Override
  public AudioTrackInfo decorateTrackInfo(AudioTrackInfo trackInfo) {
    return trackInfo;
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo) {
    return new TwitchVideoPlayback(httpInterfaceManager, loadPlaybackInfo(trackInfo));
  }

  @Override
  public void close() throws Exception {
    httpInterfaceManager.close();
  }

  private TwitchVideoPlaybackInfo loadPlaybackInfo(AudioTrackInfo trackInfo) {
    String videoId = trackInfo.getIdentifier();

    long cachedTime = trackInfo.getLongProperty(PROPERTY_TIME);
    String cachedToken = trackInfo.getStringProperty(PROPERTY_TOKEN);
    String cachedSignature = trackInfo.getStringProperty(PROPERTY_SIGNATURE);

    if (cachedToken != null && cachedSignature != null && cachedTime + CACHE_DURATION >= System.currentTimeMillis()) {
      TwitchVideoSession session = sessionManager.getSession();

      if (cachedToken.contains(session.deviceId)) {
        return new TwitchVideoPlaybackInfo(videoId, cachedSignature, cachedToken);
      }
    }

    VideoInfo streamInfo = loadStreamInfo(videoId);

    return new TwitchVideoPlaybackInfo(videoId, streamInfo.signature, streamInfo.token);
  }

  private AudioInfoEntity loadVideo(String videoId, AudioTrackInfoTemplate template) {
    VideoInfo streamInfo = loadStreamInfo(videoId);

    return AudioTrackInfoBuilder
        .fromTemplate(template)
        .with(sourceProperty)
        .with(coreIdentifier(videoId))
        .with(coreAuthor(streamInfo.ownerDisplayName))
        .with(coreTitle(streamInfo.title))
        .with(coreLength(streamInfo.lengthSeconds * 1000L))
        .with(coreIsStream(false))
        .with(custom(PROPERTY_TIME, PLAYBACK_CACHE.mask, System.currentTimeMillis()))
        .with(custom(PROPERTY_TOKEN, PLAYBACK_CACHE.mask, streamInfo.token))
        .with(custom(PROPERTY_SIGNATURE, PLAYBACK_CACHE.mask, streamInfo.signature))
        .build();
  }

  private VideoInfo loadStreamInfo(String videoId) {
    TwitchVideoSession session = sessionManager.getSession();

    try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
      HttpPost request = new HttpPost("https://gql.twitch.tv/gql");
      request.setHeader("Client-ID", session.clientId);
      request.setHeader("Device-ID", session.deviceId);
      request.setEntity(new StringEntity(formatGqlPayload(videoId), plainContentType));

      JsonBrowser result = HttpClientTools.fetchResponseAsJson(httpInterface, request, false);

      return buildStreamInfo(result);
    } catch (IOException e) {
      throw ExceptionTools.toRuntimeException(e);
    }
  }

  private VideoInfo buildStreamInfo(JsonBrowser result) {
    JsonBrowser videoInfo = result.index(1).get("data").get("video");
    String ownerDisplayName = videoInfo.get("owner").get("displayName").text();
    String title = videoInfo.get("title").text();
    long lengthSeconds = videoInfo.get("lengthSeconds").asLong(-1);

    if (ownerDisplayName == null || title == null || lengthSeconds == -1) {
      throw new RuntimeException("Missing info fields in response.");
    }

    JsonBrowser tokenHolder = result.index(0).get("data").get("videoPlaybackAccessToken");
    String token = tokenHolder.get("value").text();
    String signature = tokenHolder.get("signature").text();

    if (token == null || signature == null) {
      throw new RuntimeException("No token info in response.");
    }

    return new VideoInfo(ownerDisplayName, title, lengthSeconds, signature, token);
  }

  private static String formatGqlPayload(String videoId) {
    return "[{\"operationName\":\"PlaybackAccessToken_Template\",\"query\":\"query PlaybackAccessToken_Template" +
        "($login: String!, $isLive: Boolean!, $vodID: ID!, $isVod: Boolean!, $playerType: String!) { " +
        "streamPlaybackAccessToken(channelName: $login, params: {platform: \\\"web\\\", " +
        "playerBackend: \\\"mediaplayer\\\", playerType: $playerType}) @include(if: $isLive) { " +
        "value signature __typename }  videoPlaybackAccessToken(id: $vodID, params: {platform: \\\"web\\\", " +
        "playerBackend: \\\"mediaplayer\\\", playerType: $playerType}) @include(if: $isVod) { " +
        "value signature __typename }}\",\"variables\":{\"isLive\": false,\"login\": \"\",\"isVod\": true," +
        "\"vodID\": \"" + videoId + "\",\"playerType\": \"site\"}},{\"operationName\":\"ComscoreStreamingQuery\"," +
        "\"variables\":{\"channel\":\"\",\"clipSlug\":\"\",\"isClip\":false,\"isLive\":false," +
        "\"isVodOrCollection\":true,\"vodID\":\"" + videoId + "\"},\"extensions\":{\"persistedQuery\":{\"version\":1," +
        "\"sha256Hash\": \"e1edae8122517d013405f237ffcc124515dc6ded82480a88daef69c83b53ac01\"}}}]";
  }

  private static String extractVideoIdFromUrl(String hint) {
    Matcher matcher = urlPattern.matcher(hint);

    if (!matcher.matches()) {
      return null;
    }

    return matcher.group(1);
  }

  private static class VideoInfo {
    private final String ownerDisplayName;
    private final String title;
    private final long lengthSeconds;
    private final String signature;
    private final String token;

    private VideoInfo(String ownerDisplayName, String title, long lengthSeconds, String signature, String token) {
      this.ownerDisplayName = ownerDisplayName;
      this.title = title;
      this.lengthSeconds = lengthSeconds;
      this.signature = signature;
      this.token = token;
    }
  }
}
