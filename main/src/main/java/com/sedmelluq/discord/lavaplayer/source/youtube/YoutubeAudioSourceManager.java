package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.http.ExtendedHttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.http.MultiHttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.FAULT;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

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
  private static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<Map<String, String>>() {};

  private static final Pattern directVideoIdPattern = Pattern.compile("^" + VIDEO_ID_REGEX + "$");

  private final Extractor[] extractors = new Extractor[] {
      new Extractor(directVideoIdPattern, id -> loadTrackWithVideoId(id, false)),
      new Extractor(Pattern.compile("^" + PLAYLIST_ID_REGEX + "$"), id -> loadPlaylistWithId(id, null)),
      new Extractor(Pattern.compile("^" + PROTOCOL_REGEX + DOMAIN_REGEX + "/.*"), this::loadFromMainDomain),
      new Extractor(Pattern.compile("^" + PROTOCOL_REGEX + SHORT_DOMAIN_REGEX + "/.*"), this::loadFromShortDomain)
  };

  private final YoutubeSignatureResolver signatureResolver;
  private final HttpInterfaceManager httpInterfaceManager;
  private final ExtendedHttpConfigurable combinedHttpConfiguration;
  private final YoutubeMixProvider mixProvider;
  private final boolean allowSearch;
  private final YoutubeTrackDetailsLoader trackDetailsLoader;
  private final YoutubeSearchResultLoader searchResultLoader;
  private volatile int playlistPageCount;

  /**
   * Create an instance with default settings.
   */
  public YoutubeAudioSourceManager() {
    this(true);
  }

  /**
   * Create an instance.
   * @param allowSearch Whether to allow search queries as identifiers
   */
  public YoutubeAudioSourceManager(boolean allowSearch) {
    this(
        allowSearch,
        new DefaultYoutubeTrackDetailsLoader(),
        new YoutubeSearchProvider(),
        new YoutubeSignatureCipherManager()
    );
  }

  public YoutubeAudioSourceManager(
      boolean allowSearch,
      YoutubeTrackDetailsLoader trackDetailsLoader,
      YoutubeSearchResultLoader searchResultLoader,
      YoutubeSignatureResolver signatureResolver
  ) {
    httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
    httpInterfaceManager.setHttpContextFilter(new YoutubeHttpContextFilter());

    this.allowSearch = allowSearch;
    playlistPageCount = 6;
    mixProvider = new YoutubeMixProvider(this);

    this.trackDetailsLoader = trackDetailsLoader;
    this.signatureResolver = signatureResolver;
    this.searchResultLoader = searchResultLoader;

    combinedHttpConfiguration = new MultiHttpConfigurable(Arrays.asList(
        httpInterfaceManager,
        searchResultLoader.getHttpConfiguration()
    ));
  }

  public YoutubeTrackDetailsLoader getTrackDetailsLoader() {
    return trackDetailsLoader;
  }

  public YoutubeSignatureResolver getSignatureResolver() {
    return signatureResolver;
  }

  /**
   * @param playlistPageCount Maximum number of pages loaded from one playlist. There are 100 tracks per page.
   */
  public void setPlaylistPageCount(int playlistPageCount) {
    this.playlistPageCount = playlistPageCount;
  }

  /**
   * @param maximumPoolSize Maximum number of threads in mix loader thread pool.
   */
  public void setMixLoaderMaximumPoolSize(int maximumPoolSize) {
    mixProvider.setLoaderMaximumPoolSize(maximumPoolSize);
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

  /**
   * @return Get an HTTP interface for a playing track.
   */
  public HttpInterface getHttpInterface() {
    return httpInterfaceManager.getInterface();
  }

  @Override
  public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
    combinedHttpConfiguration.configureRequests(configurator);
  }

  @Override
  public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
    combinedHttpConfiguration.configureBuilder(configurator);
  }

  public ExtendedHttpConfigurable getHttpConfiguration() {
    return combinedHttpConfiguration;
  }

  public ExtendedHttpConfigurable getMainHttpConfiguration() {
    return httpInterfaceManager;
  }

  public ExtendedHttpConfigurable getSearchHttpConfiguration() {
    return searchResultLoader.getHttpConfiguration();
  }

  private AudioItem loadItemOnce(AudioReference reference) {
    if (allowSearch && reference.identifier.startsWith(SEARCH_PREFIX)) {
      return searchResultLoader.loadSearchResult(
          reference.identifier.substring(SEARCH_PREFIX.length()).trim(),
          this::buildTrackFromInfo
      );
    }

    return loadNonSearch(reference.identifier);
  }

  /**
   * Loads a single track from video ID.
   *
   * @param videoId ID of the YouTube video.
   * @param mustExist True if it should throw an exception on missing track, otherwise returns AudioReference.NO_TRACK.
   * @return Loaded YouTube track.
   */
  public AudioItem loadTrackWithVideoId(String videoId, boolean mustExist) {
    try (HttpInterface httpInterface = getHttpInterface()) {
      YoutubeTrackDetails details = trackDetailsLoader.loadDetails(httpInterface, videoId);

      if (details == null) {
        if (mustExist) {
          throw new FriendlyException("Video unavailable", COMMON, null);
        } else {
          return AudioReference.NO_TRACK;
        }
      }

      return new YoutubeAudioTrack(details.getTrackInfo(), this);
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions("Loading information for a YouTube track failed.", FAULT, e);
    }
  }

  /**
   * @param tracks List of tracks to search from.
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

    if (!alerts.isNull()) throw new FriendlyException(alerts.index(0).safeGet("alertRenderer").safeGet("text").safeGet("simpleText").text(), COMMON, null);

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
      return "/browse_ajax" + "?continuation=" + continuationsToken + "&ctoken=" + continuationsToken + "&hl=en";
    }

    return null;
  }

  /**
   * @param videoId Video ID. Used as {@link AudioTrackInfo#identifier}.
   * @param title See {@link AudioTrackInfo#title}.
   * @param uploader Name of the uploader. Used as {@link AudioTrackInfo#author}.
   * @param isStream See {@link AudioTrackInfo#isStream}.
   * @param duration See {@link AudioTrackInfo#length}.
   * @return An audio track instance.
   */
  public YoutubeAudioTrack buildTrackObject(String videoId, String title, String uploader, boolean isStream, long duration) {
    return new YoutubeAudioTrack(new AudioTrackInfo(title, uploader, duration, videoId, isStream, getWatchUrl(videoId)), this);
  }

  private YoutubeAudioTrack buildTrackFromInfo(AudioTrackInfo info) {
    return new YoutubeAudioTrack(info, this);
  }

  private static String getWatchUrl(String videoId) {
    return "https://www.youtube.com/watch?v=" + videoId;
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
