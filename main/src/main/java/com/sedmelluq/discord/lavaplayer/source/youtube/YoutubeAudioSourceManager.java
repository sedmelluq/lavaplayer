package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.DaemonThreadFactory;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.ExecutorTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sedmelluq.discord.lavaplayer.tools.DataFormatTools.convertToMapLayout;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.FAULT;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager that implements finding Youtube videos or playlists based on an URL or ID.
 */
public class YoutubeAudioSourceManager implements AudioSourceManager {
  private static final Logger log = LoggerFactory.getLogger(YoutubeAudioSourceManager.class);
  static final String CHARSET = "UTF-8";

  private static final String VIDEO_ID_REGEX = "([a-zA-Z0-9_-]{11})";
  private static final String PLAYLIST_REGEX = "((PL|LL|FL)[a-zA-Z0-9_-]+)";
  private static final String MIX_REGEX = "(RD[a-zA-Z0-9_-]+)";
  private static final String PROTOCOL_REGEX = "(?:http://|https://|)";
  private static final String SUFFIX_REGEX = "(?:\\?.*|&.*|)";
  private static final String SEARCH_PREFIX = "ytsearch:";

  private static final Pattern[] validTrackPatterns = new Pattern[] {
      Pattern.compile("^" + VIDEO_ID_REGEX + "$"),
      Pattern.compile("^" + PROTOCOL_REGEX + "(?:www\\.|)youtube.com/watch\\?v=" + VIDEO_ID_REGEX + SUFFIX_REGEX + "$"),
      Pattern.compile("^" + PROTOCOL_REGEX + "(?:www\\.|)youtu.be/" + VIDEO_ID_REGEX + SUFFIX_REGEX + "$")
  };

  private static final Pattern[] validPlaylistPatterns = new Pattern[] {
      Pattern.compile("^" + PLAYLIST_REGEX + "$"),
      Pattern.compile("^" + PROTOCOL_REGEX + "(?:www\\.|)youtube.com/playlist\\?list=" + PLAYLIST_REGEX + SUFFIX_REGEX + "$")
  };

  private static final String LIST_PARAMETER = "&list=";
  private static final Pattern playlistEmbeddedPattern = Pattern.compile(LIST_PARAMETER + PLAYLIST_REGEX);
  private static final Pattern mixEmbeddedPattern = Pattern.compile(LIST_PARAMETER + MIX_REGEX);

  private final HttpClientBuilder httpClientBuilder;
  private final YoutubeSignatureCipherManager signatureCipherManager;
  private final ExecutorService mixLoadingExecutor;
  private final boolean allowSearch;

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
    httpClientBuilder = createSharedCookiesHttpBuilder();
    signatureCipherManager = new YoutubeSignatureCipherManager();
    mixLoadingExecutor = new ThreadPoolExecutor(0, 10, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new DaemonThreadFactory("yt-mix"));
    this.allowSearch = allowSearch;
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
      if (HttpClientTools.isConnectionResetException(exception.getCause())) {
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
    ExecutorTools.shutdownExecutor(mixLoadingExecutor, "youtube mix");
  }

  /**
   * @return A new HttpClient instance. All instances returned from this method use the same cookie jar.
   */
  public CloseableHttpClient createHttpClient() {
    return httpClientBuilder.build();
  }

  public YoutubeSignatureCipherManager getCipherManager() {
    return signatureCipherManager;
  }

  private static HttpClientBuilder createSharedCookiesHttpBuilder() {
    CookieStore cookieStore = new BasicCookieStore();
    HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
    httpClientBuilder.setDefaultCookieStore(cookieStore);
    return httpClientBuilder;
  }

  private AudioItem loadItemOnce(AudioReference reference) {
    AudioItem result;

    if (allowSearch && reference.identifier.startsWith(SEARCH_PREFIX)) {
      return loadSearchResult(reference.identifier.substring(SEARCH_PREFIX.length()).trim());
    }

    if ((result = loadTrack(reference.identifier)) == null) {
      result = loadPlaylist(reference.identifier);
    }

    return result;
  }

  private AudioItem loadTrack(String identifier) {
    for (Pattern pattern : validTrackPatterns) {
      Matcher matcher = pattern.matcher(identifier);

      if (matcher.matches()) {
        Matcher playlistMatcher = playlistEmbeddedPattern.matcher(identifier);
        Matcher mixMatcher = mixEmbeddedPattern.matcher(identifier);

        if (playlistMatcher.find()) {
          return loadLinkedPlaylistWithId(playlistMatcher.group(1), matcher.group(1));
        } else if (mixMatcher.find()) {
          return loadMixWithId(mixMatcher.group(1), matcher.group(1));
        } else {
          return loadTrackWithVideoId(matcher.group(1), false);
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

  private AudioItem loadTrackWithVideoId(String videoId, boolean mustExist) {
    try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
      JsonBrowser info = getTrackInfoFromMainPage(httpClient, videoId, mustExist);
      if (info == null) {
        return AudioReference.NO_TRACK;
      }

      JsonBrowser args = info.get("args");
      AudioTrackInfo trackInfo = new AudioTrackInfo(
          args.get("title").text(), args.get("author").text(), args.get("length_seconds").as(Integer.class) * 1000, videoId, false
      );

      return new YoutubeAudioTrack(trackInfo, this);
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions("Loading information for a YouTube track failed.", FAULT, e);
    }
  }

  JsonBrowser getTrackInfoFromMainPage(CloseableHttpClient httpClient, String videoId, boolean mustExist) throws Exception {
    try (CloseableHttpResponse response = httpClient.execute(new HttpGet("https://www.youtube.com/watch?v=" + videoId))) {
      if (response.getStatusLine().getStatusCode() != 200) {
        throw new IOException("Invalid status code for video page response.");
      }

      String html = IOUtils.toString(response.getEntity().getContent(), Charset.forName(CHARSET));
      String configJson = DataFormatTools.extractBetween(html, "ytplayer.config = ", ";ytplayer.load");

      if (configJson != null) {
        return JsonBrowser.parse(configJson);
      }
    }

    if (determineFailureReason(httpClient, videoId, mustExist)) {
      return null;
    }

    // In case main page does not give player configuration, but info page indicates an OK result, it is probably an
    // age-restricted video for which the complete track info can be combined from the embed page and the info page.
    return getTrackInfoFromEmbedPage(httpClient, videoId);
  }

  private boolean determineFailureReason(CloseableHttpClient httpClient, String videoId, boolean mustExist) throws Exception {
    try (CloseableHttpResponse response = httpClient.execute(new HttpGet("https://www.youtube.com/get_video_info?hl=en_GB&video_id=" + videoId))) {
      if (response.getStatusLine().getStatusCode() != 200) {
        throw new IOException("Invalid status code for video info response.");
      }

      Map<String, String> format = convertToMapLayout(URLEncodedUtils.parse(response.getEntity()));
      return determineFailureReasonFromStatus(format.get("status"), format.get("reason"), mustExist);
    }
  }

  private boolean determineFailureReasonFromStatus(String status, String reason, boolean mustExist) {
    if ("fail".equals(status)) {
      if ("This video does not exist.".equals(reason) && !mustExist) {
        return true;
      } else if (reason != null) {
        throw new FriendlyException(reason, COMMON, null);
      }
    } else if ("ok".equals(status)) {
      return false;
    }

    throw new FriendlyException("Track is unavailable for an unknown reason.", SUSPICIOUS,
        new IllegalStateException("Main page had no video, but video info has no error."));
  }

  private JsonBrowser getTrackInfoFromEmbedPage(CloseableHttpClient httpClient, String videoId) throws Exception {
    JsonBrowser basicInfo = loadTrackBaseInfoFromEmbedPage(httpClient, videoId);
    basicInfo.put("args", loadTrackArgsFromVideoInfoPage(httpClient, videoId, basicInfo.get("sts").text()));
    return basicInfo;
  }

  private JsonBrowser loadTrackBaseInfoFromEmbedPage(CloseableHttpClient httpClient, String videoId) throws Exception {
    try (CloseableHttpResponse response = httpClient.execute(new HttpGet("https://www.youtube.com/embed/" + videoId))) {
      if (response.getStatusLine().getStatusCode() != 200) {
        throw new IOException("Invalid status code for embed video page response.");
      }

      String html = IOUtils.toString(response.getEntity().getContent(), Charset.forName(CHARSET));
      String configJson = DataFormatTools.extractBetween(html, "'PLAYER_CONFIG': ", ",'EXPERIMENT_FLAGS'");

      if (configJson != null) {
        return JsonBrowser.parse(configJson);
      }
    }

    throw new FriendlyException("Track information is unavailable.", SUSPICIOUS,
        new IllegalStateException("Expected player config is not present in embed page."));
  }

  private Map<String, String> loadTrackArgsFromVideoInfoPage(CloseableHttpClient httpClient, String videoId, String sts) throws Exception {
    String url = "https://www.youtube.com/get_video_info?hl=en_GB&video_id=" + videoId + "&sts=" + sts;

    try (CloseableHttpResponse response = httpClient.execute(new HttpGet(url))) {
      if (response.getStatusLine().getStatusCode() != 200) {
        throw new IOException("Invalid status code for video info response.");
      }

      return convertToMapLayout(URLEncodedUtils.parse(response.getEntity()));
    }
  }

  private AudioPlaylist loadPlaylist(String identifier) {
    for (Pattern pattern : validPlaylistPatterns) {
      Matcher matcher = pattern.matcher(identifier);

      if (matcher.matches()) {
        return loadPlaylistWithId(matcher.group(1), null);
      }
    }

    return null;
  }

  private AudioPlaylist loadPlaylistWithId(String playlistId, String selectedVideoId) {
    log.debug("Starting to load playlist with ID {}", playlistId);

    try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
      try (CloseableHttpResponse response = httpClient.execute(new HttpGet("https://www.youtube.com/playlist?list=" + playlistId))) {
        if (response.getStatusLine().getStatusCode() != 200) {
          throw new IOException("Invalid status code for playlist response.");
        }

        Document document = Jsoup.parse(response.getEntity().getContent(), CHARSET, "");
        return buildPlaylist(httpClient, document, selectedVideoId);
      }
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions(e);
    }
  }

  private AudioPlaylist buildPlaylist(CloseableHttpClient httpClient, Document document, String selectedVideoId) throws IOException {
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

    // Also load the next pages, each result gives us a JSON with separate values for list html and next page loader html
    while (loadMoreUrl != null && ++loadCount <= 5) {
      try (CloseableHttpResponse response = httpClient.execute(new HttpGet("https://www.youtube.com" + loadMoreUrl))) {
        if (response.getStatusLine().getStatusCode() != 200) {
          throw new IOException("Invalid status code for playlist response.");
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

  private AudioTrack findSelectedTrack(List<AudioTrack> tracks, String selectedVideoId) {
    if (selectedVideoId != null) {
      for (AudioTrack track : tracks) {
        if (selectedVideoId.equals(track.getIdentifier())) {
          return track;
        }
      }
    }
    return null;
  }

  private String extractPlaylistTracks(Element videoContainer, Element loadMoreContainer, List<AudioTrack> tracks) {
    for (Element video : videoContainer.select(".pl-video")) {
      Elements lengthElements = video.select(".timestamp span");

      // If the timestamp element does not exist, it means the video is private
      if (!lengthElements.isEmpty()) {
        String videoId = video.attr("data-video-id").trim();
        String title = video.attr("data-title").trim();
        String author = video.select(".pl-video-owner a").text().trim();

        int lengthInSeconds = lengthTextToSeconds(lengthElements.first().text());

        AudioTrackInfo info = new AudioTrackInfo(title, author, lengthInSeconds * 1000, videoId, false);
        tracks.add(new YoutubeAudioTrack(info, this));
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

  private static int lengthTextToSeconds(String durationText) {
    int length = 0;

    for (String part : durationText.split(":")) {
      length = length * 60 + Integer.valueOf(part);
    }

    return length;
  }

  private AudioPlaylist loadMixWithId(String mixId, String selectedVideoId) {
    List<String> videoIds = new ArrayList<>();

    try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
      String mixUrl = "https://www.youtube.com/watch?v=" + selectedVideoId + "&list=" + mixId;

      try (CloseableHttpResponse response = httpClient.execute(new HttpGet(mixUrl))) {
        if (response.getStatusLine().getStatusCode() != 200) {
          throw new IOException("Invalid status code for mix response.");
        }

        Document document = Jsoup.parse(response.getEntity().getContent(), CHARSET, "");
        extractVideoIdsFromMix(document, videoIds);
      }
    } catch (IOException e) {
      throw new FriendlyException("Could not read mix page.", SUSPICIOUS, e);
    }

    if (videoIds.isEmpty()) {
      throw new FriendlyException("Could not find tracks from mix.", SUSPICIOUS, null);
    }

    return loadTracksAsynchronously(videoIds, selectedVideoId);
  }

  private void extractVideoIdsFromMix(Document document, List<String> videoIds) {
    for (Element videoList : document.select("#playlist-autoscroll-list")) {
      for (Element item : videoList.select("li")) {
        videoIds.add(item.attr("data-video-id"));
      }
    }
  }

  private AudioPlaylist loadTracksAsynchronously(List<String> videoIds, String selectedVideoId) {
    ExecutorCompletionService<AudioItem> completion = new ExecutorCompletionService<>(mixLoadingExecutor);
    List<AudioTrack> tracks = new ArrayList<>();

    for (final String videoId : videoIds) {
      completion.submit(() -> loadTrackWithVideoId(videoId, true));
    }

    try {
      fetchTrackResultsFromExecutor(completion, tracks, videoIds.size());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    AudioTrack selectedTrack = findSelectedTrack(tracks, selectedVideoId);

    if (tracks.isEmpty()) {
      throw new FriendlyException("No tracks from the mix loaded succesfully.", SUSPICIOUS, null);
    } else if (selectedTrack == null) {
      throw new FriendlyException("The selected track of the mix failed to load.", SUSPICIOUS, null);
    }

    return new BasicAudioPlaylist("YouTube mix", tracks, selectedTrack, false);
  }

  private void fetchTrackResultsFromExecutor(ExecutorCompletionService<AudioItem> completion, List<AudioTrack> tracks, int size) throws InterruptedException {
    for (int i = 0; i < size; i++) {
      try {
        AudioItem item = completion.take().get();

        if (item instanceof AudioTrack) {
          tracks.add((AudioTrack) item);
        }
      } catch (ExecutionException e) {
        log.warn("Failed to load a track from a mix.", e);
      }
    }
  }

  private AudioItem loadSearchResult(String query) {
    log.debug("Performing a search with query {}", query);

    try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
      URI url = new URIBuilder("https://www.youtube.com/results").addParameter("search_query", query).build();

      try (CloseableHttpResponse response = httpClient.execute(new HttpGet(url))) {
        if (response.getStatusLine().getStatusCode() != 200) {
          throw new IOException("Invalid status code for search response.");
        }

        Document document = Jsoup.parse(response.getEntity().getContent(), CHARSET, "");
        return extractSearchResults(document, query);
      }
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions(e);
    }
  }

  private AudioItem extractSearchResults(Document document, String query) {
    List<AudioTrack> tracks = new ArrayList<>();

    for (Element results : document.select("#page > #content #results")) {
      for (Element result : results.select(".yt-lockup-video")) {
        extractTrackFromResultEntry(tracks, result);
      }
    }

    if (tracks.isEmpty()) {
      return AudioReference.NO_TRACK;
    } else {
      return new BasicAudioPlaylist("Search results for: " + query, tracks, null, true);
    }
  }

  private void extractTrackFromResultEntry(List<AudioTrack> tracks, Element element) {
    Element durationElement = element.select(".video-time").first();
    Element contentElement = element.select(".yt-lockup-content").first();
    String videoId = element.attr("data-context-item-id");

    if (durationElement == null || contentElement == null || videoId.isEmpty()) {
      return;
    }

    long length = lengthTextToSeconds(durationElement.text()) * 1000L;

    String title = contentElement.select(".yt-lockup-title > a").text();
    String author = contentElement.select(".yt-lockup-byline > a").text();

    tracks.add(new YoutubeAudioTrack(new AudioTrackInfo(title, author, length, videoId, false), this));
  }
}
