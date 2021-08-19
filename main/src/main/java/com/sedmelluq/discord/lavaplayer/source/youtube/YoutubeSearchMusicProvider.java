package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.http.ExtendedHttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Handles processing YouTube Music searches.
 */
public class YoutubeSearchMusicProvider implements YoutubeSearchMusicResultLoader {
  private static final Logger log = LoggerFactory.getLogger(YoutubeSearchMusicProvider.class);

  private static final String WATCH_URL_PREFIX = "https://www.youtube.com/watch?v=";
  private static final String YT_MUSIC_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30";
  private static final String YT_MUSIC_PAYLOAD = "{\"context\":{\"client\":{\"clientName\":\"WEB_REMIX\",\"clientVersion\":\"1.20210726.00.01\"}},\"query\":\"%s\"}";
  private final HttpInterfaceManager httpInterfaceManager;
  private final Pattern ytMusicDataRegex = Pattern.compile("<body>\\s*(.*)\\s*</body>");

  public YoutubeSearchMusicProvider() {
    this.httpInterfaceManager = HttpClientTools.createCookielessThreadLocalManager();
    httpInterfaceManager.setHttpContextFilter(new BaseYoutubeHttpContextFilter());
  }

  public ExtendedHttpConfigurable getHttpConfiguration() {
    return httpInterfaceManager;
  }

  /**
   * @param query Search query.
   * @return Playlist of the first page of music results.
   */
  @Override
  public AudioItem loadSearchMusicResult(String query, Function<AudioTrackInfo, AudioTrack> trackFactory) {
    log.debug("Performing a search music with query {}", query);

    try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
      URI url = new URIBuilder("https://music.youtube.com/youtubei/v1/search")
          .addParameter("alt", "json")
          .addParameter("key", YT_MUSIC_KEY).build();

      HttpPost post = new HttpPost(url);
      StringEntity payload = new StringEntity(String.format(YT_MUSIC_PAYLOAD, query.replace("\"", "\\\"")), "UTF-8");
      post.setHeader("Referer", "music.youtube.com");
      post.setEntity(payload);
      try (CloseableHttpResponse response = httpInterface.execute(post)) {
        HttpClientTools.assertSuccessWithContent(response, "search music response");

        Document document = Jsoup.parse(response.getEntity().getContent(), StandardCharsets.UTF_8.name(), "");
        return extractSearchResults(document, query, trackFactory);
      }
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions(e);
    }
  }

  private AudioItem extractSearchResults(Document document, String query,
                                         Function<AudioTrackInfo, AudioTrack> trackFactory) {

    List<AudioTrack> tracks;
    try {
      tracks = extractMusicTracks(document, trackFactory);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (tracks.isEmpty()) {
      return AudioReference.NO_TRACK;
    } else {
      return new BasicAudioPlaylist("Search music results for: " + query, tracks, null, true);
    }
  }

  private List<AudioTrack> extractMusicTracks(Document document, Function<AudioTrackInfo, AudioTrack> trackFactory) throws IOException {
    Matcher matcher = ytMusicDataRegex.matcher(document.outerHtml());
    if (!matcher.find()) {
      log.warn("Failed to match ytMusicData JSON object");
      return Collections.emptyList();
    }

    JsonBrowser jsonBrowser = JsonBrowser.parse(matcher.group(1));
    ArrayList<AudioTrack> list = new ArrayList<>();
    jsonBrowser.get("contents")
        .get("tabbedSearchResultsRenderer")
        .get("tabs")
        .index(0)
        .get("tabRenderer")
        .get("content")
        .get("sectionListRenderer")
        .get("contents")
        .values().forEach(json -> {
          List<AudioTrack> tracks = extractMusicData(json, trackFactory);
          if (!tracks.isEmpty()) list.addAll(tracks);
        });
    return list;
  }

  private List<AudioTrack> extractMusicData(JsonBrowser json, Function<AudioTrackInfo, AudioTrack> trackFactory) {
    List<JsonBrowser> contents = json.get("musicShelfRenderer").get("contents").values();
    if (contents.isEmpty()) {
      // Doesn't include essential info
      return Collections.emptyList();
    }
    return contents.stream().map(content -> extractTrackInfo(content, trackFactory))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private AudioTrack extractTrackInfo(JsonBrowser content, Function<AudioTrackInfo, AudioTrack> trackFactory) {
    List<List<JsonBrowser>> columns = content.get("musicResponsiveListItemRenderer")
        .get("flexColumns")
        .values()
        .stream()
        .map(column -> column.get("musicResponsiveListItemFlexColumnRenderer")
            .get("text")
            .get("runs")
            .values())
        .collect(Collectors.toList());
    JsonBrowser firstColumn = columns.get(0).get(0);
    List<JsonBrowser> secondColumn = columns.get(1);

    String type = secondColumn.get(0).get("text").text();
    if (!"Song".equals(type) && !"Video".equals(type)) return null;

    String identifier = firstColumn.get("navigationEndpoint").get("watchEndpoint").get("videoId").text();
    long duration = DataFormatTools.durationTextToMillis(secondColumn.get(secondColumn.size() - 1).get("text").text());
    String artist = secondColumn.stream().filter(this::getIsArtist)
        .map(column -> column.get("text").text())
        .collect(Collectors.joining(" & "));
    if (artist.isEmpty()) artist = "Unknown artist";

    AudioTrackInfo info = new AudioTrackInfo(firstColumn.get("text").text(),
        artist,
        duration,
        identifier,
        false,
        WATCH_URL_PREFIX + identifier
    );
    return trackFactory.apply(info);
  }

  private boolean getIsArtist(JsonBrowser browser) {
    String type = browser.get("navigationEndpoint")
        .get("browseEndpoint")
        .get("browseEndpointContextSupportedConfigs")
        .get("browseEndpointContextMusicConfig")
        .get("pageType")
        .text();
    if (type == null) return false;
    return type.equals("MUSIC_PAGE_TYPE_ARTIST") || type.equals("MUSIC_PAGE_TYPE_USER_CHANNEL");
  }
}
