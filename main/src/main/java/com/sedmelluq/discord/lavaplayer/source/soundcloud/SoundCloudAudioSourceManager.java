package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import java.io.DataInput;
import java.io.DataOutput;
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

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager that implements finding SoundCloud tracks based on URL.
 */
public class SoundCloudAudioSourceManager implements AudioSourceManager, HttpConfigurable {
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
  public String getSourceName() {
    return "soundcloud";
  }

  @Override
  public AudioItem loadItem(DefaultAudioPlayerManager manager, AudioReference reference) {
    AudioItem track = processAsSingleTrack(reference);

    if (track == null) {
      track = playlistLoader.load(reference.identifier, httpInterfaceManager, this::buildTrackFromInfo);
    }

    if (track == null) {
      track = processAsLikedTracks(reference);
    }

    if (track == null && allowSearch) {
      track = processAsSearchQuery(reference);
    }

    return track;
  }

  @Override
  public boolean isTrackEncodable(AudioTrack track) {
    return true;
  }

  @Override
  public void encodeTrack(AudioTrack track, DataOutput output) {
    // No extra information to save
  }

  @Override
  public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) {
    return new SoundCloudAudioTrack(trackInfo, this);
  }

  @Override
  public void shutdown() {
    // Nothing to shut down
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

  private AudioTrack processAsSingleTrack(AudioReference reference) {
    String url = SoundCloudHelper.nonMobileUrl(reference.identifier);

    Matcher trackUrlMatcher = trackUrlPattern.matcher(url);
    if (trackUrlMatcher.matches() && !"likes".equals(trackUrlMatcher.group(2))) {
      return loadFromTrackPage(url);
    }

    Matcher unlistedUrlMatcher = unlistedUrlPattern.matcher(url);
    if (unlistedUrlMatcher.matches()) {
      return loadFromTrackPage(url);
    }

    return null;
  }

  private AudioItem processAsLikedTracks(AudioReference reference) {
    String url = SoundCloudHelper.nonMobileUrl(reference.identifier);

    if (likedUrlPattern.matcher(url).matches()) {
      return loadFromLikedTracks(url);
    } else {
      return null;
    }
  }

  public AudioTrack loadFromTrackPage(String trackWebUrl) {
    try (HttpInterface httpInterface = getHttpInterface()) {
      JsonBrowser rootData = htmlDataLoader.load(httpInterface, trackWebUrl);
      JsonBrowser trackData = dataReader.findTrackData(rootData);
      return loadFromTrackData(trackData);
    } catch (IOException e) {
      throw new FriendlyException("Loading track from SoundCloud failed.", SUSPICIOUS, e);
    }
  }

  protected AudioTrack loadFromTrackData(JsonBrowser trackData) {
    SoundCloudTrackFormat format = formatHandler.chooseBestFormat(dataReader.readTrackFormats(trackData));
    return buildTrackFromInfo(dataReader.readTrackInfo(trackData, formatHandler.buildFormatIdentifier(format)));
  }

  private AudioTrack buildTrackFromInfo(AudioTrackInfo trackInfo) {
    return new SoundCloudAudioTrack(trackInfo, this);
  }

  private AudioItem loadFromLikedTracks(String likedListUrl) {
    try (HttpInterface httpInterface = getHttpInterface()) {
      UserInfo userInfo = findUserIdFromLikedList(httpInterface, likedListUrl);
      if (userInfo == null) {
        return AudioReference.NO_TRACK;
      }

      return extractTracksFromLikedList(loadLikedListForUserId(httpInterface, userInfo), userInfo);
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

  private AudioItem extractTracksFromLikedList(JsonBrowser likedTracks, UserInfo userInfo) {
    List<AudioTrack> tracks = new ArrayList<>();

    for (JsonBrowser item : likedTracks.get("collection").values()) {
      JsonBrowser trackItem = item.get("track");

      if (!trackItem.isNull() && !dataReader.isTrackBlocked(trackItem)) {
        tracks.add(loadFromTrackData(trackItem));
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

  private AudioItem processAsSearchQuery(AudioReference reference) {
    if (reference.identifier.startsWith(SEARCH_PREFIX)) {
      if (reference.identifier.startsWith(SEARCH_PREFIX_DEFAULT)) {
        return loadSearchResult(reference.identifier.substring(SEARCH_PREFIX_DEFAULT.length()).trim(), 0, DEFAULT_SEARCH_RESULTS);
      }

      Matcher searchMatcher = searchPattern.matcher(reference.identifier);

      if (searchMatcher.matches()) {
        return loadSearchResult(searchMatcher.group(3), Integer.parseInt(searchMatcher.group(1)), Integer.parseInt(searchMatcher.group(2)));
      }
    }

    return null;
  }

  private AudioItem loadSearchResult(String query, int offset, int rawLimit) {
    int limit = Math.min(rawLimit, MAXIMUM_SEARCH_RESULTS);

    try (
        HttpInterface httpInterface = getHttpInterface();
        CloseableHttpResponse response = httpInterface.execute(new HttpGet(buildSearchUri(query, offset, limit)))
    ) {
      return loadSearchResultsFromResponse(response, query);
    } catch (IOException e) {
      throw new FriendlyException("Loading search results from SoundCloud failed.", SUSPICIOUS, e);
    }
  }

  private AudioItem loadSearchResultsFromResponse(HttpResponse response, String query) throws IOException {
    try {
      JsonBrowser searchResults = JsonBrowser.parse(response.getEntity().getContent());
      return extractTracksFromSearchResults(query, searchResults);
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

  private AudioItem extractTracksFromSearchResults(String query, JsonBrowser searchResults) {
    List<AudioTrack> tracks = new ArrayList<>();

    for (JsonBrowser item : searchResults.get("collection").values()) {
      if (!item.isNull()) {
        tracks.add(loadFromTrackData(item));
      }
    }

    return new BasicAudioPlaylist("Search results for: " + query, tracks, null, true);
  }
}
