package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.http.HttpRequestModifier;
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
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.FAULT;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;
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
  private static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<Map<String, String>>() {};

  private static final Pattern directVideoIdPattern = Pattern.compile("^" + VIDEO_ID_REGEX + "$");

  private final Extractor[] extractors = new Extractor[] {
      new Extractor(directVideoIdPattern, id -> loadTrackWithVideoId(id, false)),
      new Extractor(Pattern.compile("^" + PLAYLIST_ID_REGEX + "$"), id -> loadPlaylistWithId(id, null)),
      new Extractor(Pattern.compile("^" + PROTOCOL_REGEX + DOMAIN_REGEX + "/.*"), this::loadFromMainDomain),
      new Extractor(Pattern.compile("^" + PROTOCOL_REGEX + SHORT_DOMAIN_REGEX + "/.*"), this::loadFromShortDomain)
  };

  private final YoutubeSignatureCipherManager signatureCipherManager;
  private final HttpInterfaceManager httpInterfaceManager;
  private final YoutubeSearchProvider searchProvider;
  private final YoutubeMixProvider mixProvider;
  private final boolean allowSearch;
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
    signatureCipherManager = new YoutubeSignatureCipherManager();

    httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager(request -> {
      request.setHeader("x-youtube-client-name", "1");
      request.setHeader("x-youtube-client-version", "2.20191008.04.01");
    });

    this.allowSearch = allowSearch;
    playlistPageCount = 6;
    searchProvider = new YoutubeSearchProvider(this);
    mixProvider = new YoutubeMixProvider(this);
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

  public YoutubeSignatureCipherManager getCipherManager() {
    return signatureCipherManager;
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
   * @param videoId ID of the YouTube video.
   * @param mustExist True if it should throw an exception on missing track, otherwise returns AudioReference.NO_TRACK.
   * @return Loaded YouTube track.
   */
  public AudioItem loadTrackWithVideoId(String videoId, boolean mustExist) {
    try (HttpInterface httpInterface = getHttpInterface()) {
      HttpGet request = new HttpGet("https://www.youtube.com/watch?v=" + videoId + "&pbj=1");

      JsonBrowser info = getTrackInfoFromMainPage(httpInterface, videoId, mustExist);
      if (info == null) {
        return AudioReference.NO_TRACK;
      }

      JsonBrowser args = info.get("args");
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

  /**
   * @param httpInterface HTTP interface to use for performing any necessary request.
   * @param videoId ID of the video.
   * @param mustExist If <code>true</code>, throws an exception instead of returning <code>null</code> if the track does
   *                  not exist.
   * @return JSON information about the track if it exists. <code>null</code> if it does not and mustExist is
   *         <code>false</code>.
   * @throws IOException On network error.
   */
  public JsonBrowser getTrackInfoFromMainPage(HttpInterface httpInterface, String videoId, boolean mustExist) throws IOException {
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

        for (JsonBrowser child : json.values()) {
          if (child.isMap()) {
            if (!child.get("player").isNull()) {
              playerInfo = child.get("player");
            } else if (!child.get("playerResponse").isNull()) {
              statusBlock = child.get("playerResponse").safeGet("playabilityStatus");
            }
          }
        }

        if (!checkStatusBlock(statusBlock, mustExist)) {
          return null;
        } else if (playerInfo == null || playerInfo.isNull()) {
          throw new RuntimeException("No player info block.");
        }

        return playerInfo;
      } catch (Exception e) {
        throw new FriendlyException("Received unexpected response from YouTube.", SUSPICIOUS,
            new RuntimeException("Failed to parse: " + responseText, e));
      }
    }
  }

  private boolean checkStatusBlock(JsonBrowser statusBlock, boolean mustExist) {
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
        throw new FriendlyException(reason, COMMON, null);
      }
    } else {
      throw new FriendlyException("This video cannot be viewed anonymously.", COMMON, null);
    }
  }

  private AudioPlaylist loadPlaylistWithId(String playlistId, String selectedVideoId) {
    log.debug("Starting to load playlist with ID {}", playlistId);

    try (HttpInterface httpInterface = getHttpInterface()) {
      try (CloseableHttpResponse response = httpInterface.execute(new HttpGet("https://www.youtube.com/playlist?list=" + playlistId))) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
          throw new IOException("Invalid status code for playlist response: " + statusCode);
        }

        Document document = Jsoup.parse(response.getEntity().getContent(), CHARSET, "");
        return buildPlaylist(httpInterface, document, selectedVideoId);
      }
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions(e);
    }
  }

  private AudioPlaylist buildPlaylist(HttpInterface httpInterface, Document document, String selectedVideoId) throws IOException {
    boolean isAccessible = !document.select("#pl-header").isEmpty();

    if (!isAccessible) {
      if (selectedVideoId != null) {
        return null;
      } else {
        throw new FriendlyException("The playlist is private.", COMMON, null);
      }
    }

    Element container = document.select("#pl-header").first().parent();

    String playlistName = container.select(".pl-header-title").first().text();

    List<AudioTrack> tracks = new ArrayList<>();
    String loadMoreUrl = extractPlaylistTracks(container, container, tracks);
    int loadCount = 0;
    int pageCount = playlistPageCount;

    // Also load the next pages, each result gives us a JSON with separate values for list html and next page loader html
    while (loadMoreUrl != null && ++loadCount < pageCount) {
      try (CloseableHttpResponse response = httpInterface.execute(new HttpGet("https://www.youtube.com" + loadMoreUrl))) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
          throw new IOException("Invalid status code for playlist response: " + statusCode);
        }

        JsonBrowser json = JsonBrowser.parse(response.getEntity().getContent());

        String html = json.get("content_html").text();
        Element videoContainer = Jsoup.parse("<table>" + html + "</table>", "");

        String moreHtml = json.get("load_more_widget_html").text();
        Element moreContainer = moreHtml != null ? Jsoup.parse(moreHtml) : null;

        loadMoreUrl = extractPlaylistTracks(videoContainer, moreContainer, tracks);
      }
    }

    return new BasicAudioPlaylist(playlistName, tracks, findSelectedTrack(tracks, selectedVideoId), false);
  }

  private String extractPlaylistTracks(Element videoContainer, Element loadMoreContainer, List<AudioTrack> tracks) {
    for (Element video : videoContainer.select(".pl-video")) {
      Elements lengthElements = video.select(".timestamp span");

      // If the timestamp element does not exist, it means the video is private
      if (!lengthElements.isEmpty()) {
        String videoId = video.attr("data-video-id").trim();
        String title = video.attr("data-title").trim();
        String author = video.select(".pl-video-owner a").text().trim();
        long duration = DataFormatTools.durationTextToMillis(lengthElements.first().text());

        tracks.add(buildTrackObject(videoId, title, author, false, duration));
      }
    }

    if (loadMoreContainer != null) {
      Elements more = loadMoreContainer.select(".load-more-button");
      if (!more.isEmpty()) {
        return more.first().attr("data-uix-load-more-href");
      }
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
    return new YoutubeAudioTrack(new AudioTrackInfo(title, uploader, duration, videoId, isStream, getWatchUrl(videoId),
        Collections.singletonMap("artworkUrl", getArtworkUrl(videoId))), this);
  }

  private static String getWatchUrl(String videoId) {
    return "https://www.youtube.com/watch?v=" + videoId;
  }

  private static String getArtworkUrl(String videoId) {
    return String.format("https://img.youtube.com/vi/%s/0.jpg", videoId);
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
