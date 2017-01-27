package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager that implements finding SoundCloud tracks based on URL.
 */
public class SoundCloudAudioSourceManager implements AudioSourceManager {
  private static final Logger log = LoggerFactory.getLogger(SoundCloudAudioSourceManager.class);

  private static final int DEFAULT_SEARCH_RESULTS = 10;
  private static final int MAXIMUM_SEARCH_RESULTS = 200;

  private static final String CHARSET = "UTF-8";
  private static final String CLIENT_ID = "fDoItMDbsbZz8dY16ZzARCZmzgHBPotA";
  private static final String TRACK_URL_REGEX = "^(?:http://|https://|)(?:www\\.|)soundcloud\\.com/([a-zA-Z0-9-_]+)/([a-zA-Z0-9-_]+)(?:\\?.*|)$";
  private static final String UNLISTED_URL_REGEX = "^(?:http://|https://|)(?:www\\.|)soundcloud\\.com/([a-zA-Z0-9-_]+)/([a-zA-Z0-9-_]+)/s-([a-zA-Z0-9-_]+)(?:\\?.*|)$";
  private static final String PLAYLIST_URL_REGEX = "^(?:http://|https://|)(?:www\\.|)soundcloud\\.com/([a-zA-Z0-9-_]+)/sets/([a-zA-Z0-9-_]+)(?:\\?.*|)$";
  private static final String LIKED_URL_REGEX = "^(?:http://|https://|)(?:www\\.|)soundcloud\\.com/([a-zA-Z0-9-_]+)/likes/?(?:\\?.*|)$";
  private static final String LIKED_USER_URN_REGEX = "\"urn\":\"soundcloud:users:([0-9]+)\",\"username\":\"([^\"]+)\"";
  private static final String SEARCH_PREFIX = "scsearch";
  private static final String SEARCH_PREFIX_DEFAULT = "scsearch:";
  private static final String SEARCH_REGEX = SEARCH_PREFIX + "\\[([0-9]{1,9}),([0-9]{1,9})\\]:\\s*(.*)\\s*";

  private static final Pattern trackUrlPattern = Pattern.compile(TRACK_URL_REGEX);
  private static final Pattern unlistedUrlPattern = Pattern.compile(UNLISTED_URL_REGEX);
  private static final Pattern playlistUrlPattern = Pattern.compile(PLAYLIST_URL_REGEX);
  private static final Pattern likedUrlPattern = Pattern.compile(LIKED_URL_REGEX);
  private static final Pattern likedUserUrnPattern = Pattern.compile(LIKED_USER_URN_REGEX);
  private static final Pattern searchPattern = Pattern.compile(SEARCH_REGEX);

  private final HttpClientBuilder httpClientBuilder;
  private final boolean allowSearch;

  /**
   * Create an instance with default settings.
   */
  public SoundCloudAudioSourceManager() {
    this(true);
  }

  /**
   * Create an instance.
   * @param allowSearch Whether to allow search queries as identifiers
   */
  public SoundCloudAudioSourceManager(boolean allowSearch) {
    httpClientBuilder = HttpClientTools.createSharedCookiesHttpBuilder();
    this.allowSearch = allowSearch;
  }


  @Override
  public String getSourceName() {
    return "soundcloud";
  }

  @Override
  public AudioItem loadItem(DefaultAudioPlayerManager manager, AudioReference reference) {
    AudioItem track = processAsSingleTrack(reference);

    if (track == null) {
      track = processAsPlaylist(reference);
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
  public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
    // No extra information to save
  }

  @Override
  public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
    return new SoundCloudAudioTrack(trackInfo, this);
  }

  @Override
  public void shutdown() {
    // Nothing to shut down
  }

  /**
   * @return A new HttpClient instance. All instances returned from this method use the same cookie jar.
   */
  public CloseableHttpClient createHttpClient() {
    return httpClientBuilder.build();
  }

  /**
   * @param trackId ID of the track
   * @return URL to use for streaming the track.
   */
  public String getTrackUrlFromId(String trackId) {
    String[] parts = trackId.split("\\|");

    if (parts.length < 2) {
      return "https://api.soundcloud.com/tracks/" + trackId + "/stream?client_id=" + CLIENT_ID;
    } else {
      return "https://api.soundcloud.com/tracks/" + parts[0] + "/stream?client_id=" + CLIENT_ID + "&secret_token=" + parts[1];
    }
  }

  private AudioTrack processAsSingleTrack(AudioReference reference) {
    Matcher trackUrlMatcher = trackUrlPattern.matcher(reference.identifier);
    if (trackUrlMatcher.matches() && !"likes".equals(trackUrlMatcher.group(2))) {
      return loadFromTrackPage(reference.identifier, null);
    }

    Matcher unlistedUrlMatcher = unlistedUrlPattern.matcher(reference.identifier);
    if (unlistedUrlMatcher.matches()) {
      return loadFromTrackPage(reference.identifier, "s-" + unlistedUrlMatcher.group(3));
    }

    return null;
  }

  private AudioItem processAsPlaylist(AudioReference reference) {
    if (playlistUrlPattern.matcher(reference.identifier).matches()) {
      return loadFromSet(reference.identifier);
    } else if (likedUrlPattern.matcher(reference.identifier).matches()) {
      return loadFromLikedTracks(reference.identifier);
    } else {
      return null;
    }
  }

  private AudioTrack loadFromTrackPage(String trackWebUrl, String secretToken) {
    try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
      JsonBrowser trackInfoJson = loadTrackInfoFromJson(loadPageConfigJson(httpClient, trackWebUrl));
      return buildAudioTrack(trackInfoJson, secretToken);
    } catch (IOException e) {
      throw new FriendlyException("Loading track from SoundCloud failed.", SUSPICIOUS, e);
    }
  }

  private AudioTrack buildAudioTrack(JsonBrowser trackInfoJson, String secretToken) {
    String trackId = trackInfoJson.get("id").text();

    AudioTrackInfo trackInfo = new AudioTrackInfo(
        trackInfoJson.get("title").text(),
        trackInfoJson.get("user").get("username").text(),
        trackInfoJson.get("duration").as(Integer.class),
        secretToken != null ? trackId + "|" + secretToken : trackId,
        false
    );

    return new SoundCloudAudioTrack(trackInfo, this);
  }

  private JsonBrowser loadPageConfigJson(CloseableHttpClient httpClient, String url) throws IOException {
    try (CloseableHttpResponse response = httpClient.execute(new HttpGet(url))) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode == 404) {
        throw new FriendlyException("That track does not exist.", COMMON, null);
      } else if (statusCode != 200) {
        throw new IOException("Invalid status code for video page response.");
      }

      String html = IOUtils.toString(response.getEntity().getContent(), Charset.forName(CHARSET));
      String configJson = DataFormatTools.extractBetween(html, "e}var c=", ",o=Date.now()");

      if (configJson == null) {
        throw new FriendlyException("This url does not appear to be a playable track.", SUSPICIOUS, null);
      }

      return JsonBrowser.parse(configJson);
    }
  }

  private JsonBrowser loadTrackInfoFromJson(JsonBrowser json) {
    for (JsonBrowser value : json.values()) {
      for (JsonBrowser entry : value.safeGet("data").values()) {
        if (entry.isMap() && "track".equals(entry.get("kind").text())) {
          return entry;
        }
      }
    }

    throw new IllegalStateException("Could not find track information block.");
  }

  private AudioPlaylist loadFromSet(String playlistWebUrl) {
    try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
      JsonBrowser playlistInfo = loadPlaylistInfoFromJson(loadPageConfigJson(httpClient, playlistWebUrl));

      return new BasicAudioPlaylist(
          playlistInfo.get("title").text(),
          loadTracksFromPlaylist(httpClient, playlistInfo, playlistWebUrl),
          null,
          false
      );
    } catch (IOException e) {
      throw new FriendlyException("Loading playlist from SoundCloud failed.", SUSPICIOUS, e);
    }
  }

  private JsonBrowser loadPlaylistInfoFromJson(JsonBrowser json) {
    for (JsonBrowser value : json.values()) {
      for (JsonBrowser entry : value.safeGet("data").values()) {
        if (entry.isMap() && "playlist".equals(entry.get("kind").text())) {
          return entry;
        }
      }
    }

    throw new IllegalStateException("Could not find playlist information block.");
  }

  private List<AudioTrack> loadTracksFromPlaylist(CloseableHttpClient httpClient, JsonBrowser playlistInfo, String playlistWebUrl) throws IOException {
    List<AudioTrack> tracks = new ArrayList<>();
    List<String> trackIds = loadPlaylistTrackList(playlistInfo);
    URI trackListUrl = buildTrackListUrl(trackIds);

    try (CloseableHttpResponse response = httpClient.execute(new HttpGet(trackListUrl))) {
      if (response.getStatusLine().getStatusCode() != 200) {
        throw new IOException("Invalid status code for track list response.");
      }

      JsonBrowser trackList = JsonBrowser.parse(response.getEntity().getContent());
      int blockedCount = 0;

      for (JsonBrowser trackInfoJson : trackList.values()) {
        if ("BLOCK".equals(trackInfoJson.get("policy").text())) {
          blockedCount++;
        } else {
          tracks.add(buildAudioTrack(trackInfoJson, null));
        }
      }

      if (blockedCount > 0) {
        log.debug("In soundcloud playlist {}, {} tracks were omitted because they are blocked.", playlistWebUrl, blockedCount);
      }
    }

    sortPlaylistTracks(tracks, trackIds);

    return tracks;
  }

  private List<String> loadPlaylistTrackList(JsonBrowser playlistInfo) {
    List<String> trackIds = new ArrayList<>();
    for (JsonBrowser trackInfo : playlistInfo.get("tracks").values()) {
      trackIds.add(trackInfo.get("id").text());
    }
    return trackIds;
  }

  private URI buildTrackListUrl(List<String> trackIds) {
    try {
      StringJoiner joiner = new StringJoiner(",");
      for (String trackId : trackIds) {
        joiner.add(trackId);
      }

      return new URIBuilder("https://api-v2.soundcloud.com/tracks")
          .addParameter("ids", joiner.toString())
          .addParameter("client_id", CLIENT_ID)
          .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private static void sortPlaylistTracks(List<AudioTrack> tracks, List<String> trackIds) {
    final Map<String, Integer> positions = new HashMap<>();
    for (int i = 0; i < trackIds.size(); i++) {
      positions.put(trackIds.get(i), i);
    }

    Collections.sort(tracks, (o1, o2) -> Integer.compare(
        getSortPosition(positions, o1),
        getSortPosition(positions, o2)
    ));
  }

  private static int getSortPosition(Map<String, Integer> positions, AudioTrack track) {
    return DataFormatTools.defaultOnNull(positions.get(track.getIdentifier()), Integer.MAX_VALUE);
  }

  private AudioItem loadFromLikedTracks(String likedListUrl) {
    try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
      UserInfo userInfo = findUserIdFromLikedList(httpClient, likedListUrl);
      if (userInfo == null) {
        return AudioReference.NO_TRACK;
      }

      return extractTracksFromLikedList(loadLikedListForUserId(httpClient, userInfo), userInfo);
    } catch (IOException e) {
      throw new FriendlyException("Loading liked tracks from SoundCloud failed.", SUSPICIOUS, e);
    }
  }

  private UserInfo findUserIdFromLikedList(CloseableHttpClient httpClient, String likedListUrl) throws IOException {
    try (CloseableHttpResponse response = httpClient.execute(new HttpGet(likedListUrl))) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode == 404) {
        return null;
      } else if (statusCode != 200) {
        throw new IOException("Invalid status code for track list response.");
      }

      Matcher matcher = likedUserUrnPattern.matcher(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
      return matcher.find() ? new UserInfo(matcher.group(1), matcher.group(2)) : null;
    }
  }

  private JsonBrowser loadLikedListForUserId(CloseableHttpClient httpClient, UserInfo userInfo) throws IOException {
    HttpUriRequest request = new HttpGet("https://api-v2.soundcloud.com/users/" + userInfo.id + "/likes?client_id="
        + CLIENT_ID + "&limit=200&offset=0");

    try (CloseableHttpResponse response = httpClient.execute(request)) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode != 200) {
        throw new IOException("Invalid status code for liked tracks response.");
      }

      return JsonBrowser.parse(response.getEntity().getContent());
    }
  }

  private AudioItem extractTracksFromLikedList(JsonBrowser likedTracks, UserInfo userInfo) {
    List<AudioTrack> tracks = new ArrayList<>();

    for (JsonBrowser item : likedTracks.get("collection").values()) {
      JsonBrowser trackItem = item.get("track");

      if (!trackItem.isNull()) {
        tracks.add(buildAudioTrack(trackItem, null));
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

    try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
      return loadSearchResultsFromUrl(httpClient, query, buildSearchUri(query, offset, limit));
    } catch (IOException e) {
      throw new FriendlyException("Loading search results from SoundCloud failed.", SUSPICIOUS, e);
    }
  }

  private AudioItem loadSearchResultsFromUrl(HttpClient httpClient, String query, URI uri) throws IOException {
    HttpResponse response = httpClient.execute(new HttpGet(uri));

    try {
      JsonBrowser searchResults = JsonBrowser.parse(response.getEntity().getContent());
      return extractTracksFromSearchResults(query, searchResults);
    } finally {
      EntityUtils.consumeQuietly(response.getEntity());
    }
  }

  private static URI buildSearchUri(String query, int offset, int limit) {
    try {
      return new URIBuilder("https://api-v2.soundcloud.com/search/tracks")
          .addParameter("q", query)
          .addParameter("client_id", CLIENT_ID)
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
        tracks.add(buildAudioTrack(item, null));
      }
    }

    return new BasicAudioPlaylist("Search results for: " + query, tracks, null, true);
  }
}
