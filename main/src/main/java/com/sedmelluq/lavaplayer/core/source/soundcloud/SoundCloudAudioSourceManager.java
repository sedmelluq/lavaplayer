package com.sedmelluq.lavaplayer.core.source.soundcloud;

import com.sedmelluq.lavaplayer.core.http.HttpClientTools;
import com.sedmelluq.lavaplayer.core.http.HttpConfigurable;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.HttpInterfaceManager;
import com.sedmelluq.lavaplayer.core.info.AudioInfoEntity;
import com.sedmelluq.lavaplayer.core.info.playlist.BasicAudioPlaylist;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.request.generic.GenericAudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoBuilder;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoTemplate;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.source.AudioSource;
import com.sedmelluq.lavaplayer.core.tools.JsonBrowser;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager that implements finding SoundCloud tracks based on URL.
 */
public class SoundCloudAudioSourceManager implements AudioSource, HttpConfigurable {
  private static final int DEFAULT_SEARCH_RESULTS = 10;
  private static final int MAXIMUM_SEARCH_RESULTS = 200;

  private static final String TRACK_URL_REGEX = "^(?:http://|https://|)(?:www\\.|)(?:m\\.|)soundcloud\\.com/([a-zA-Z0-9-_]+)/([a-zA-Z0-9-_]+)(?:\\?.*|)$";
  private static final String UNLISTED_URL_REGEX = "^(?:http://|https://|)(?:www\\.|)(?:m\\.|)soundcloud\\.com/([a-zA-Z0-9-_]+)/([a-zA-Z0-9-_]+)/s-([a-zA-Z0-9-_]+)(?:\\?.*|)$";
  private static final String LIKED_URL_REGEX = "^(?:http://|https://|)(?:www\\.|)(?:m\\.|)soundcloud\\.com/([a-zA-Z0-9-_]+)/likes/?(?:\\?.*|)$";
  private static final String LIKED_USER_URN_REGEX = "\"urn\":\"soundcloud:users:([0-9]+)\",\"username\":\"([^\"]+)\"";
  private static final String SEARCH_PREFIX = "scsearch";
  private static final String SEARCH_PREFIX_DEFAULT = "scsearch:";
  private static final String SEARCH_REGEX = SEARCH_PREFIX + "\\[([0-9]{1,9}),([0-9]{1,9})\\]:\\s*(.*)\\s*";

  private static final Pattern trackUrlPattern = Pattern.compile(TRACK_URL_REGEX);
  private static final Pattern unlistedUrlPattern = Pattern.compile(UNLISTED_URL_REGEX);
  private static final Pattern likedUrlPattern = Pattern.compile(LIKED_URL_REGEX);
  private static final Pattern likedUserUrnPattern = Pattern.compile(LIKED_USER_URN_REGEX);
  private static final Pattern searchPattern = Pattern.compile(SEARCH_REGEX);

  private final SoundCloudDataReader dataReader;
  private final SoundCloudHtmlDataLoader htmlDataLoader;
  private final SoundCloudFormatHandler formatHandler;
  private final SoundCloudPlaylistLoader playlistLoader;
  private final HttpInterfaceManager httpInterfaceManager;
  private final SoundCloudClientIdTracker clientIdTracker;
  private final boolean allowSearch;

  public static SoundCloudAudioSourceManager createDefault() {
    SoundCloudDataReader dataReader = new DefaultSoundCloudDataReader();
    SoundCloudHtmlDataLoader htmlDataLoader = new DefaultSoundCloudHtmlDataLoader();
    SoundCloudFormatHandler formatHandler = new DefaultSoundCloudFormatHandler();

    return new SoundCloudAudioSourceManager(true, dataReader, htmlDataLoader, formatHandler,
        new DefaultSoundCloudPlaylistLoader(htmlDataLoader, dataReader, formatHandler));
  }

  /**
   * Create an instance.
   * @param allowSearch Whether to allow search queries as identifiers
   */
  public SoundCloudAudioSourceManager(
      boolean allowSearch,
      SoundCloudDataReader dataReader,
      SoundCloudHtmlDataLoader htmlDataLoader,
      SoundCloudFormatHandler formatHandler,
      SoundCloudPlaylistLoader playlistLoader
  ) {
    this.allowSearch = allowSearch;
    this.dataReader = dataReader;
    this.htmlDataLoader = htmlDataLoader;
    this.formatHandler = formatHandler;
    this.playlistLoader = playlistLoader;

    httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
    clientIdTracker = new SoundCloudClientIdTracker(httpInterfaceManager);
    httpInterfaceManager.setHttpContextFilter(new SoundCloudHttpContextFilter(clientIdTracker));
  }

  public SoundCloudFormatHandler getFormatHandler() {
    return formatHandler;
  }

  @Override
  public String getName() {
    return SoundCloudHelper.NAME;
  }

  @Override
  public AudioInfoEntity loadItem(AudioInfoRequest request) {
    if (!(request instanceof GenericAudioInfoRequest)) {
      return null;
    }

    String hint = ((GenericAudioInfoRequest) request).getHint();
    AudioInfoEntity track = processAsSingleTrack(hint, request);

    if (track == null) {
      track = playlistLoader.load(hint, httpInterfaceManager, request);
    }

    if (track == null) {
      track = processAsLikedTracks(hint, request);
    }

    if (track == null && allowSearch) {
      track = processAsSearchQuery(hint, request);
    }

    return track;
  }

  @Override
  public AudioTrackInfo decorateTrackInfo(AudioTrackInfo trackInfo) {
    return trackInfo;
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo) {
    return new SoundCloudUrlPlayback(trackInfo.getIdentifier(), this);
  }

  @Override
  public void close() {
    ExceptionTools.closeWithWarnings(httpInterfaceManager);
  }

  public String getClientId() {
    return clientIdTracker.getClientId();
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

  private AudioTrackInfo processAsSingleTrack(String hint, AudioTrackInfoTemplate template) {
    String url = SoundCloudHelper.nonMobileUrl(hint);

    Matcher trackUrlMatcher = trackUrlPattern.matcher(url);
    if (trackUrlMatcher.matches() && !"likes".equals(trackUrlMatcher.group(2))) {
      return loadFromTrackPage(url, template);
    }

    Matcher unlistedUrlMatcher = unlistedUrlPattern.matcher(url);
    if (unlistedUrlMatcher.matches()) {
      return loadFromTrackPage(url, template);
    }

    return null;
  }

  private AudioInfoEntity processAsLikedTracks(String hint, AudioTrackInfoTemplate template) {
    String url = SoundCloudHelper.nonMobileUrl(hint);

    if (likedUrlPattern.matcher(url).matches()) {
      return loadFromLikedTracks(url, template);
    } else {
      return null;
    }
  }

  protected AudioTrackInfo loadFromTrackPage(String trackWebUrl, AudioTrackInfoTemplate template) {
    try (HttpInterface httpInterface = getHttpInterface()) {
      JsonBrowser rootData = htmlDataLoader.load(httpInterface, trackWebUrl);
      JsonBrowser trackData = dataReader.findTrackData(rootData);
      return loadFromTrackData(trackData, template);
    } catch (IOException e) {
      throw new FriendlyException("Loading track from SoundCloud failed.", SUSPICIOUS, e);
    }
  }

  public String loadPlaybackIdentifier(HttpInterface httpInterface, String trackWebUrl) throws IOException {
    JsonBrowser rootData = htmlDataLoader.load(httpInterface, trackWebUrl);
    JsonBrowser trackData = dataReader.findTrackData(rootData);

    SoundCloudTrackFormat format = formatHandler.chooseBestFormat(dataReader.readTrackFormats(trackData));
    return formatHandler.buildFormatIdentifier(format);
  }

  protected AudioTrackInfo loadFromTrackData(JsonBrowser trackData, AudioTrackInfoTemplate template) {
    SoundCloudTrackFormat format = formatHandler.chooseBestFormat(dataReader.readTrackFormats(trackData));

    AudioTrackInfoBuilder builder = AudioTrackInfoBuilder.fromTemplate(template)
        .with(SoundCloudHelper.sourceProperty);

    dataReader.readTrackInfo(trackData, formatHandler.buildFormatIdentifier(format), builder);
    return builder.build();
  }

  private AudioInfoEntity loadFromLikedTracks(String likedListUrl, AudioTrackInfoTemplate template) {
    try (HttpInterface httpInterface = getHttpInterface()) {
      UserInfo userInfo = findUserIdFromLikedList(httpInterface, likedListUrl);
      if (userInfo == null) {
        return AudioInfoEntity.NO_INFO;
      }

      return extractTracksFromLikedList(loadLikedListForUserId(httpInterface, userInfo), userInfo, template);
    } catch (IOException e) {
      throw new FriendlyException("Loading liked tracks from SoundCloud failed.", SUSPICIOUS, e);
    }
  }

  private UserInfo findUserIdFromLikedList(HttpInterface httpInterface, String likedListUrl) throws IOException {
    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(likedListUrl))) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode == HttpStatus.SC_NOT_FOUND) {
        return null;
      } else if (!HttpClientTools.isSuccessWithContent(statusCode)) {
        throw new IOException("Invalid status code for track list response: " + statusCode);
      }

      Matcher matcher = likedUserUrnPattern.matcher(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
      return matcher.find() ? new UserInfo(matcher.group(1), matcher.group(2)) : null;
    }
  }

  private JsonBrowser loadLikedListForUserId(HttpInterface httpInterface, UserInfo userInfo) throws IOException {
    URI uri = URI.create("https://api-v2.soundcloud.com/users/" + userInfo.id + "/likes?limit=200&offset=0");

    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(uri))) {
      HttpClientTools.assertSuccessWithContent(response, "liked tracks response");
      return JsonBrowser.parse(response.getEntity().getContent());
    }
  }

  private AudioInfoEntity extractTracksFromLikedList(
      JsonBrowser likedTracks,
      UserInfo userInfo,
      AudioTrackInfoTemplate template
  ) {
    List<AudioTrackInfo> tracks = new ArrayList<>();

    for (JsonBrowser item : likedTracks.get("collection").values()) {
      JsonBrowser trackItem = item.get("track");

      if (!trackItem.isNull()) {
        tracks.add(loadFromTrackData(trackItem, template));
      }
    }

    return new BasicAudioPlaylist("Liked by " + userInfo.name, tracks, null, false);
  }

  private static class UserInfo {
    private final String id;
    private final String name;

    private UserInfo(String id, String name) {
      this.id = id;
      this.name = name;
    }
  }

  private AudioInfoEntity processAsSearchQuery(String hint, AudioTrackInfoTemplate template) {
    if (hint.startsWith(SEARCH_PREFIX)) {
      if (hint.startsWith(SEARCH_PREFIX_DEFAULT)) {
        return loadSearchResult(hint.substring(SEARCH_PREFIX_DEFAULT.length()).trim(), 0,
            DEFAULT_SEARCH_RESULTS, template);
      }

      Matcher searchMatcher = searchPattern.matcher(hint);

      if (searchMatcher.matches()) {
        return loadSearchResult(searchMatcher.group(3), Integer.parseInt(searchMatcher.group(1)),
            Integer.parseInt(searchMatcher.group(2)), template);
      }
    }

    return null;
  }

  private AudioInfoEntity loadSearchResult(String query, int offset, int rawLimit, AudioTrackInfoTemplate template) {
    int limit = Math.min(rawLimit, MAXIMUM_SEARCH_RESULTS);

    try (
        HttpInterface httpInterface = getHttpInterface();
        CloseableHttpResponse response = httpInterface.execute(new HttpGet(buildSearchUri(query, offset, limit)))
    ) {
      return loadSearchResultsFromResponse(response, query, template);
    } catch (IOException e) {
      throw new FriendlyException("Loading search results from SoundCloud failed.", SUSPICIOUS, e);
    }
  }

  private AudioInfoEntity loadSearchResultsFromResponse(
      HttpResponse response,
      String query,
      AudioTrackInfoTemplate template
  ) throws IOException {
    try {
      JsonBrowser searchResults = JsonBrowser.parse(response.getEntity().getContent());
      return extractTracksFromSearchResults(query, searchResults, template);
    } finally {
      EntityUtils.consumeQuietly(response.getEntity());
    }
  }

  private URI buildSearchUri(String query, int offset, int limit) {
    try {
      return new URIBuilder("https://api-v2.soundcloud.com/search/tracks")
          .addParameter("q", query)
          .addParameter("offset", String.valueOf(offset))
          .addParameter("limit", String.valueOf(limit))
          .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private AudioInfoEntity extractTracksFromSearchResults(
      String query,
      JsonBrowser searchResults,
      AudioTrackInfoTemplate template
  ) {
    List<AudioTrackInfo> tracks = new ArrayList<>();

    for (JsonBrowser item : searchResults.get("collection").values()) {
      if (!item.isNull()) {
        tracks.add(loadFromTrackData(item, template));
      }
    }

    return new BasicAudioPlaylist("Search results for: " + query, tracks, null, true);
  }
}
