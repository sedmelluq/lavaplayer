package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.RateLimitException;
import com.sedmelluq.discord.lavaplayer.tools.http.AbstractRoutePlanner;
import com.sedmelluq.discord.lavaplayer.tools.http.HttpRequestModifier;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.*;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.BindException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.*;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Audio source manager that implements finding Youtube videos or playlists based on an URL or ID.
 */
public class YoutubeAudioSourceManager implements AudioSourceManager, HttpConfigurable {
  private static final Logger log = LoggerFactory.getLogger(YoutubeAudioSourceManager.class);
  static final String CHARSET = "UTF-8";

  private static final String PROTOCOL_REGEX = "(?:http://|https://|)";
  private static final String DOMAIN_REGEX = "(?:www\\.|m\\.|music\\.|)youtube\\.com";
  private static final String SHORT_DOMAIN_REGEX = "(?:www\\.|)youtu\\.be";
  private static final String VIDEO_ID_REGEX = "(?<v>[a-zA-Z0-9_-]{11})";
  private static final String PLAYLIST_ID_REGEX = "(?<list>(PL|LL|FL|UU)[a-zA-Z0-9_-]+)";

  private static final String SEARCH_PREFIX = "ytsearch:";
  private static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<Map<String, String>>() {
  };

  private static final Pattern directVideoIdPattern = Pattern.compile("^" + VIDEO_ID_REGEX + "$");

  private final Extractor[] extractors = new Extractor[]{
      new Extractor(directVideoIdPattern, id -> loadTrackWithVideoId(id, false)),
      new Extractor(Pattern.compile("^" + PLAYLIST_ID_REGEX + "$"), id -> loadPlaylistWithId(id, null)),
      new Extractor(Pattern.compile("^" + PROTOCOL_REGEX + DOMAIN_REGEX + "/.*"), this::loadFromMainDomain),
      new Extractor(Pattern.compile("^" + PROTOCOL_REGEX + SHORT_DOMAIN_REGEX + "/.*"), this::loadFromShortDomain)
  };

  private final AbstractRoutePlanner routePlanner;
  private final YoutubeSignatureCipherManager signatureCipherManager;
  private final HttpInterfaceManager httpInterfaceManager;
  private final YoutubeSearchProvider searchProvider;
  private final YoutubeMixProvider mixProvider;
  private final boolean allowSearch;
  private volatile int playlistPageCount;
  private CacheProvider cacheProvider;

  /**
   * Create an instance with default settings.
   */
  public YoutubeAudioSourceManager() {
    this(true, null);
  }

  /**
   * Create an instance.
   *
   * @param allowSearch Whether to allow search queries as identifiers
   */
  public YoutubeAudioSourceManager(boolean allowSearch) {
    this(allowSearch, null);
  }

  /**
   * Create an instance.
   *
   * @param allowSearch  Whether to allow search queries as identifiers
   * @param routePlanner An IPv6 subnet to balance requests over
   */
  public YoutubeAudioSourceManager(boolean allowSearch, AbstractRoutePlanner routePlanner) {
    this.routePlanner = routePlanner;
    signatureCipherManager = new YoutubeSignatureCipherManager();

    HttpRequestModifier requestModifier = request -> {
      if (request.getURI().toString().contains("generate_204"))
        return;
      request.setHeader("x-youtube-client-name", "1");
      request.setHeader("x-youtube-client-version", "2.20191008.04.01");
    };

    if (routePlanner == null) {
      httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager(requestModifier);
      searchProvider = new YoutubeSearchProvider(this);
    } else {
      httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager(requestModifier, routePlanner);
      searchProvider = new YoutubeSearchProvider(this, routePlanner);
    }

    this.allowSearch = allowSearch;
    playlistPageCount = 6;
    cacheProvider = new CacheProviderImpl();
    mixProvider = new YoutubeMixProvider(this);
  }

  /**
   * @param playlistPageCount Maximum number of pages loaded from one playlist. There are 100 tracks per page.
   */
  public void setPlaylistPageCount(int playlistPageCount) {
    this.playlistPageCount = playlistPageCount;
  }

  public void setCacheProvider(CacheProvider provider) {
    this.cacheProvider = provider;
  }

  /**
   * @param maximumPoolSize Maximum number of threads in mix loader thread pool.
   */
  public void setMixLoaderMaximumPoolSize(int maximumPoolSize) {
    mixProvider.setLoaderMaximumPoolSize(maximumPoolSize);
  }

  public AbstractRoutePlanner getRoutePlanner() {
    return routePlanner;
  }

  @Override
  public String getSourceName() {
    return "youtube";
  }

  @Override
  public AudioItem loadItem(DefaultAudioPlayerManager manager, AudioReference reference) {
    try {
      return loadItemOnce(reference);
    } catch (FriendlyException exception) {
      // In case of a connection reset exception, try once more.
      if (HttpClientTools.isRetriableNetworkException(exception.getCause())) {
        return loadItemOnce(reference);
      } else {
        throw exception;
      }
    }
  }

  @Override
  public boolean isTrackEncodable(AudioTrack track) {
    return true;
  }

  @Override
  public void encodeTrack(AudioTrack track, DataOutput output) {
    // No custom values that need saving
  }

  @Override
  public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) {
    return new YoutubeAudioTrack(trackInfo, this);
  }

  @Override
  public void shutdown() {
    ExceptionTools.closeWithWarnings(httpInterfaceManager);

    mixProvider.shutdown();
  }

  public YoutubeSignatureCipherManager getCipherManager() {
    return signatureCipherManager;
  }

  public CacheProvider getCacheProvider() {
    return this.cacheProvider;
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
    searchProvider.configureRequests(configurator);
  }

  @Override
  public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
    httpInterfaceManager.configureBuilder(configurator);
    searchProvider.configureBuilder(configurator);
  }

  public void setHttpRequestModifier(HttpRequestModifier modifier) {
    httpInterfaceManager.setRequestModifier(modifier);
  }

  private AudioItem loadItemOnce(AudioReference reference) {
    if (allowSearch && reference.identifier.startsWith(SEARCH_PREFIX)) {
      return searchProvider.loadSearchResult(reference.identifier.substring(SEARCH_PREFIX.length()).trim());
    }

    return loadNonSearch(reference.identifier);
  }

  /**
   * Loads a single track from video ID.
   *
   * @param videoId   ID of the YouTube video.
   * @param mustExist True if it should throw an exception on missing track, otherwise returns AudioReference.NO_TRACK.
   * @return Loaded YouTube track.
   */
  public AudioItem loadTrackWithVideoId(String videoId, boolean mustExist) {
    try (HttpInterface httpInterface = getHttpInterface()) {
      //log.info("AbstractRoutePlanner last address: {}, used query address: {}", getRoutePlanner().getLastAddress(), httpInterface.getContext().getHttpRoute().getLocalAddress());
      final YoutubeJsonResponse jsonResponse = getTrackInfoFromMainPage(httpInterface, videoId, mustExist);
      if (jsonResponse == null || jsonResponse.getPlayerInfo() == null) {
        return AudioReference.NO_TRACK;
      }

      JsonBrowser args = jsonResponse.getPlayerInfo().get("args");
      boolean useOldFormat = args.get("player_response").isNull();

      if (useOldFormat) {
        if ("fail".equals(args.get("status").text())) {
          throw new FriendlyException(args.get("reason").text(), COMMON, null);
        }

        boolean isStream = "1".equals(args.get("live_playback").text());
        long duration = isStream ? Long.MAX_VALUE : args.get("length_seconds").as(Long.class) * 1000;
        return buildTrackObject(videoId, args.get("title").text(), args.get("author").text(), isStream, duration);
      }

      JsonBrowser playerResponse = JsonBrowser.parse(args.get("player_response").text());
      JsonBrowser playabilityStatus = playerResponse.get("playabilityStatus");

      if ("ERROR".equals(playabilityStatus.get("status").text())) {
        throw new FriendlyException(playabilityStatus.get("reason").text(), COMMON, null);
      }

      JsonBrowser videoDetails = playerResponse.get("videoDetails");

      boolean isStream = videoDetails.get("isLiveContent").as(Boolean.class);
      long duration = isStream ? Long.MAX_VALUE : videoDetails.get("lengthSeconds").as(Long.class) * 1000;

      return buildTrackObject(videoId, videoDetails.get("title").text(), videoDetails.get("author").text(), isStream, duration);
    } catch (Exception e) {
      if (e instanceof BindException) {
        final AbstractRoutePlanner routePlanner = getRoutePlanner();
        if (routePlanner != null) {
          log.warn("Cannot assign requested address {}, marking address as failing and retry!", routePlanner.getLastAddress());
          routePlanner.markAddressFailing();
          return loadTrackWithVideoId(videoId, mustExist);
        }
      }
      throw ExceptionTools.wrapUnfriendlyExceptions("Loading information for a YouTube track failed.", FAULT, e);
    }
  }

  /**
   * @param tracks          List of tracks to search from.
   * @param selectedVideoId Selected track identifier.
   * @return The selected track from the track list, or null if it is not present.
   */
  public AudioTrack findSelectedTrack(List<AudioTrack> tracks, String selectedVideoId) {
    if (selectedVideoId != null) {
      for (AudioTrack track : tracks) {
        if (selectedVideoId.equals(track.getIdentifier())) {
          return track;
        }
      }
    }
    return null;
  }

  private AudioItem loadFromMainDomain(String identifier) {
    UrlInfo urlInfo = getUrlInfo(identifier, true);

    if ("/watch".equals(urlInfo.path)) {
      String videoId = urlInfo.parameters.get("v");

      if (videoId != null) {
        return loadFromUrlWithVideoId(videoId, urlInfo);
      }
    } else if ("/playlist".equals(urlInfo.path)) {
      String playlistId = urlInfo.parameters.get("list");

      if (playlistId != null) {
        return loadPlaylistWithId(playlistId, null);
      }
    } else if ("/watch_videos".equals(urlInfo.path)) {
      String videoIds = urlInfo.parameters.get("video_ids");
      if (videoIds != null) {
        return loadAnonymous(videoIds);
      }
    }

    return null;
  }

  private AudioItem loadAnonymous(String videoIds) {
    try (HttpInterface httpInterface = getHttpInterface()) {
      try (CloseableHttpResponse response = httpInterface.execute(new HttpGet("https://www.youtube.com/watch_videos?video_ids=" + videoIds))) {
        int statusCode = response.getStatusLine().getStatusCode();
        HttpClientContext context = httpInterface.getContext();
        if (statusCode != 200) {
          throw new IOException("Invalid status code for playlist response: " + statusCode);
        }
        // youtube currently transforms watch_video links into a link with a video id and a list id.
        // because thats what happens, we can simply re-process with the redirected link
        List<URI> redirects = context.getRedirectLocations();
        if (redirects != null && !redirects.isEmpty()) {
          return loadNonSearch(redirects.get(0).toString());
        } else {
          throw new FriendlyException("Unable to process youtube watch_videos link", SUSPICIOUS,
              new IllegalStateException("Expected youtube to redirect watch_videos link to a watch?v={id}&list={list_id} link, but it did not redirect at all"));
        }
      }
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions(e);
    }
  }

  private AudioItem loadFromShortDomain(String identifier) {
    UrlInfo urlInfo = getUrlInfo(identifier, true);
    return loadFromUrlWithVideoId(urlInfo.path.substring(1), urlInfo);
  }

  private AudioItem loadFromUrlWithVideoId(String videoId, UrlInfo urlInfo) {
    if (videoId.length() > 11) {
      // YouTube allows extra junk in the end, it redirects to the correct video.
      videoId = videoId.substring(0, 11);
    }

    if (!directVideoIdPattern.matcher(videoId).matches()) {
      return AudioReference.NO_TRACK;
    } else if (urlInfo.parameters.containsKey("list")) {
      String playlistId = urlInfo.parameters.get("list");

      if (playlistId.startsWith("RD")) {
        return mixProvider.loadMixWithId(playlistId, videoId);
      } else {
        return loadLinkedPlaylistWithId(urlInfo.parameters.get("list"), videoId);
      }
    } else {
      return loadTrackWithVideoId(videoId, false);
    }
  }

  private AudioItem loadNonSearch(String identifier) {
    for (Extractor extractor : extractors) {
      if (extractor.pattern.matcher(identifier).matches()) {
        AudioItem item = extractor.loader.apply(identifier);

        if (item != null) {
          return item;
        }
      }
    }

    return null;
  }

  private AudioItem loadLinkedPlaylistWithId(String playlistId, String videoId) {
    AudioPlaylist playlist = loadPlaylistWithId(playlistId, videoId);

    if (playlist == null) {
      return loadTrackWithVideoId(videoId, false);
    } else {
      return playlist;
    }
  }

  /**
   * @param httpInterface HTTP interface to use for performing any necessary request.
   * @param videoId       ID of the video.
   * @param mustExist     If <code>true</code>, throws an exception instead of returning <code>null</code> if the track does
   *                      not exist.
   * @return JSON information about the track if it exists. <code>null</code> if it does not and mustExist is
   * <code>false</code>.
   * @throws IOException On network error.
   */
  public YoutubeJsonResponse getTrackInfoFromMainPage(HttpInterface httpInterface, String videoId, boolean mustExist) throws IOException {
    checkVideoAvailability(videoId);
    String url = getWatchUrl(videoId) + "&pbj=1&hl=en";

    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(url))) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode != 200) {
        throw new IOException("Invalid status code for video page response: " + statusCode);
      }

      String responseText = EntityUtils.toString(response.getEntity(), UTF_8);

      try {
        JsonBrowser json = JsonBrowser.parse(responseText);
        JsonBrowser playerInfo = null;
        JsonBrowser statusBlock = null;
        JsonBrowser preConnectUrls = null;

        for (JsonBrowser child : json.values()) {
          if (child.isMap()) {
            if (!child.get("preconnect").isNull()) {
              preConnectUrls = child.get("preconnect");
            } else if (!child.get("player").isNull()) {
              playerInfo = child.get("player");
            } else if (!child.get("playerResponse").isNull()) {
              statusBlock = child.get("playerResponse").safeGet("playabilityStatus");
            }
          }
        }

        if (!checkStatusBlock(videoId, statusBlock, mustExist)) {
          return null;
        } else if (playerInfo == null || playerInfo.isNull()) {
          throw new RuntimeException("No player info block.");
        }


        // TESTING
        if (new Random().nextBoolean()) {
          throw new RateLimitException();
        }


        return new YoutubeJsonResponse(playerInfo, preConnectUrls);
      } catch (Exception e) {
        if (e instanceof FriendlyException)
          throw e;
        if (e instanceof JsonParseException || e instanceof RateLimitException) {
          final AbstractRoutePlanner routePlanner = getRoutePlanner();
          if (routePlanner != null) {
            log.warn("YouTube rate limit reached, marking address as failing and retry");
            routePlanner.markAddressFailing();
            return getTrackInfoFromMainPage(httpInterface, videoId, mustExist);
          }
          throw new RateLimitException("YouTube rate limit reached", e);
        }
        throw new FriendlyException("Received unexpected response from YouTube.", SUSPICIOUS,
            new RuntimeException("Failed to parse: " + responseText, e));
      }
    }
  }

  private boolean checkStatusBlock(String videoId, JsonBrowser statusBlock, boolean mustExist) {
    if (statusBlock == null || statusBlock.isNull()) {
      throw new RuntimeException("No playability status block.");
    }

    String status = statusBlock.safeGet("status").text();

    if (status == null) {
      throw new RuntimeException("No playability status field.");
    } else if ("OK".equals(status)) {
      return true;
    } else if ("ERROR".equals(status)) {
      String reason = statusBlock.safeGet("reason").text();

      if (!mustExist && "Video unavailable".equals(reason)) {
        return false;
      } else {
        cacheUnavailableVideo(videoId, reason);
        throw new FriendlyException(reason, COMMON, null);
      }
    } else if ("UNPLAYABLE".equals(status) || "LOGIN_REQUIRED".equals(status)) {
      String unplayableReason = getUnplayableReason(statusBlock);
      if (unplayableReason.equals("This video may be inappropriate for some users."))
        unplayableReason = "Unable to load age restricted video."; // The original reason may be misleading
      cacheUnavailableVideo(videoId, unplayableReason);
      throw new FriendlyException(unplayableReason, COMMON, null);
    } else {
      cacheUnavailableVideo(videoId, "This video cannot be viewed anonymously.");
      throw new FriendlyException("This video cannot be viewed anonymously.", COMMON, null);
    }
  }

  private String getUnplayableReason(JsonBrowser statusBlock) {
    JsonBrowser playerErrorMessage = statusBlock.get("errorScreen").get("playerErrorMessageRenderer");
    String unplayableReason = statusBlock.safeGet("reason").text();

    if (!playerErrorMessage.safeGet("subreason").isNull()) {
      JsonBrowser subreason = playerErrorMessage.safeGet("subreason");

      if (!subreason.safeGet("simpleText").isNull()) {
        unplayableReason = subreason.safeGet("simpleText").text();
      } else if (!subreason.safeGet("runs").isNull() && subreason.safeGet("runs").isList()) {
        StringBuilder reasonBuilder = new StringBuilder();
        subreason.safeGet("runs").values().forEach(
            item -> reasonBuilder.append(item.safeGet("text").text()).append('\n')
        );
        unplayableReason = reasonBuilder.toString();
      }
    }

    return unplayableReason;
  }

  private AudioPlaylist loadPlaylistWithId(String playlistId, String selectedVideoId) {
    log.debug("Starting to load playlist with ID {}", playlistId);

    try (HttpInterface httpInterface = getHttpInterface()) {
      try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(getPlaylistUrl(playlistId) + "&pbj=1&hl=en"))) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
          throw new IOException("Invalid status code for playlist response: " + statusCode);
        }

        JsonBrowser json = JsonBrowser.parse(response.getEntity().getContent());

        return buildPlaylist(httpInterface, json, selectedVideoId);
      }
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions(e);
    }
  }

  private AudioPlaylist buildPlaylist(HttpInterface httpInterface, JsonBrowser json, String selectedVideoId) throws IOException {
    JsonBrowser jsonResponse = json.index(1).safeGet("response");

    JsonBrowser alerts = jsonResponse.safeGet("alerts");

    if (!alerts.isNull())
      throw new FriendlyException(alerts.index(0).safeGet("alertRenderer").safeGet("text").safeGet("simpleText").text(), COMMON, null);

    JsonBrowser info = jsonResponse
        .safeGet("sidebar")
        .safeGet("playlistSidebarRenderer")
        .safeGet("items")
        .index(0)
        .safeGet("playlistSidebarPrimaryInfoRenderer");

    String playlistName = info
        .safeGet("title")
        .safeGet("runs")
        .index(0)
        .safeGet("text")
        .text();

    JsonBrowser playlistVideoList = jsonResponse
        .safeGet("contents")
        .safeGet("twoColumnBrowseResultsRenderer")
        .safeGet("tabs")
        .index(0)
        .safeGet("tabRenderer")
        .safeGet("content")
        .safeGet("sectionListRenderer")
        .safeGet("contents")
        .index(0)
        .safeGet("itemSectionRenderer")
        .safeGet("contents")
        .index(0)
        .safeGet("playlistVideoListRenderer");

    List<AudioTrack> tracks = new ArrayList<>();
    String loadMoreUrl = extractPlaylistTracks(playlistVideoList, tracks);
    int loadCount = 0;
    int pageCount = playlistPageCount;

    // Also load the next pages, each result gives us a JSON with separate values for list html and next page loader html
    while (loadMoreUrl != null && ++loadCount < pageCount) {
      try (CloseableHttpResponse response = httpInterface.execute(new HttpGet("https://www.youtube.com" + loadMoreUrl))) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
          throw new IOException("Invalid status code for playlist response: " + statusCode);
        }

        JsonBrowser continuationJson = JsonBrowser.parse(response.getEntity().getContent());

        JsonBrowser playlistVideoListPage = continuationJson.index(1)
            .safeGet("response")
            .safeGet("continuationContents")
            .safeGet("playlistVideoListContinuation");

        loadMoreUrl = extractPlaylistTracks(playlistVideoListPage, tracks);
      }
    }

    return new BasicAudioPlaylist(playlistName, tracks, findSelectedTrack(tracks, selectedVideoId), false);
  }

  private String extractPlaylistTracks(JsonBrowser playlistVideoList, List<AudioTrack> tracks) {
    JsonBrowser trackArray = playlistVideoList.safeGet("contents");

    if (trackArray.isNull()) return null;

    for (JsonBrowser track : trackArray.values()) {
      JsonBrowser item = track.safeGet("playlistVideoRenderer");

      JsonBrowser shortBylineText = item.safeGet("shortBylineText");

      // If the isPlayable property does not exist, it means the video is removed or private
      // If the shortBylineText property does not exist, it means the Track is Region blocked
      if (!item.safeGet("isPlayable").isNull() && !shortBylineText.isNull()) {
        String videoId = item.safeGet("videoId").text();
        String title = item.safeGet("title").safeGet("simpleText").text();
        String author = shortBylineText.safeGet("runs").index(0).safeGet("text").text();
        long duration = Long.parseLong(item.safeGet("lengthSeconds").text()) * 1000;
        tracks.add(buildTrackObject(videoId, title, author, false, duration));
      }
    }

    JsonBrowser continuations = playlistVideoList.safeGet("continuations");

    if (!continuations.isNull()) {
      String continuationsToken = continuations.index(0).safeGet("nextContinuationData").safeGet("continuation").text();
      return "/browse_ajax?continuation=" + continuationsToken + "&ctoken=" + continuationsToken + "&hl=en";
    }

    return null;
  }

  /**
   * @param videoId  Video ID. Used as {@link AudioTrackInfo#identifier}.
   * @param title    See {@link AudioTrackInfo#title}.
   * @param uploader Name of the uploader. Used as {@link AudioTrackInfo#author}.
   * @param isStream See {@link AudioTrackInfo#isStream}.
   * @param duration See {@link AudioTrackInfo#length}.
   * @return An audio track instance.
   */
  public YoutubeAudioTrack buildTrackObject(String videoId, String title, String uploader, boolean isStream, long duration) {
    return new YoutubeAudioTrack(new AudioTrackInfo(title, uploader, duration, videoId, isStream, getWatchUrl(videoId),
        Collections.singletonMap("artworkUrl", getArtworkUrl(videoId))), this);
  }

  private void checkVideoAvailability(String videoId) {
    if (cacheProvider != null) {

      String reason = cacheProvider.checkUnavailable(videoId);

      if (reason != null) {
        throw new FriendlyException(reason, COMMON, null);
      }
    }
  }

  private void cacheUnavailableVideo(String videoId, String reason) {
    if (cacheProvider != null) {
      cacheProvider.cacheUnavailableVideo(videoId, reason);
    }
  }

  private static String getWatchUrl(String videoId) {
    return "https://www.youtube.com/watch?v=" + videoId;
  }

  private static String getArtworkUrl(String videoId) {
    return String.format("https://img.youtube.com/vi/%s/0.jpg", videoId);
  }

  private static String getPlaylistUrl(String playlistId) {
    return "https://www.youtube.com/playlist?list=" + playlistId;
  }

  private static UrlInfo getUrlInfo(String url, boolean retryValidPart) {
    try {
      if (!url.startsWith("http://") && !url.startsWith("https://")) {
        url = "https://" + url;
      }

      URIBuilder builder = new URIBuilder(url);
      return new UrlInfo(builder.getPath(), builder.getQueryParams().stream()
          .filter(it -> it.getValue() != null)
          .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue, (a, b) -> a)));
    } catch (URISyntaxException e) {
      if (retryValidPart) {
        return getUrlInfo(url.substring(0, e.getIndex() - 1), false);
      } else {
        throw new FriendlyException("Not a valid URL: " + url, COMMON, e);
      }
    }
  }

  static class YoutubeJsonResponse {
    private final JsonBrowser playerInfo;
    private final String[] preConnectUrls;

    private YoutubeJsonResponse(final JsonBrowser player, final JsonBrowser preConnectUrls) {
      this.playerInfo = player;
      if (preConnectUrls == null) {
        this.preConnectUrls = new String[0];
        return;
      }
      final List<JsonBrowser> entries = preConnectUrls.values();
      this.preConnectUrls = new String[entries.size()];
      for (int i = 0; i < entries.size(); i++) {
        this.preConnectUrls[i] = entries.get(i).as(String.class);
      }
    }

    public JsonBrowser getPlayerInfo() {
      return playerInfo;
    }

    public String[] getPreConnectUrls() {
      return preConnectUrls;
    }
  }

  private static class UrlInfo {
    private final String path;
    private final Map<String, String> parameters;

    private UrlInfo(String path, Map<String, String> parameters) {
      this.path = path;
      this.parameters = parameters;
    }
  }

  private static class Extractor {
    private final Pattern pattern;
    private final Function<String, AudioItem> loader;

    private Extractor(Pattern pattern, Function<String, AudioItem> loader) {
      this.pattern = pattern;
      this.loader = loader;
    }
  }
}
