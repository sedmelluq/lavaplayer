package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.http.ExtendedHttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles processing YouTube Music searches.
 */
public class YoutubeSearchMusicProvider implements YoutubeSearchMusicResultLoader {
  private static final Logger log = LoggerFactory.getLogger(YoutubeSearchMusicProvider.class);

  private static final String WATCH_URL_PREFIX = "https://www.youtube.com/watch?v=";
  private static final String YT_MUSIC_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30";
  private static final String YT_MUSIC_PAYLOAD = "{\"context\":{\"client\":{\"clientName\":\"WEB_REMIX\",\"clientVersion\":\"0.1\"}},\"query\":\"%s\",\"params\":\"Eg-KAQwIARAAGAAgACgAMABqChADEAQQCRAFEAo=\"}";
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
    JsonBrowser tracks = jsonBrowser.get("contents")
            .get("tabbedSearchResultsRenderer")
            .get("tabs")
            .index(0)
            .get("tabRenderer")
            .get("content")
            .get("sectionListRenderer")
            .get("contents")
            .index(0)
            .get("musicShelfRenderer")
            .get("contents");
    if (tracks == JsonBrowser.NULL_BROWSER) {
      tracks = jsonBrowser.get("contents")
              .get("tabbedSearchResultsRenderer")
              .get("tabs")
              .index(0)
              .get("tabRenderer")
              .get("content")
              .get("sectionListRenderer")
              .get("contents")
              .index(1)
              .get("musicShelfRenderer")
              .get("contents");
    }
    tracks.values().forEach(json -> {
          AudioTrack track = extractMusicData(json, trackFactory);
          if (track != null) list.add(track);
        });
    return list;
  }

  private AudioTrack extractMusicData(JsonBrowser json, Function<AudioTrackInfo, AudioTrack> trackFactory) {
    JsonBrowser columns = json.get("musicResponsiveListItemRenderer").get("flexColumns");
    if (columns.isNull()) {
      // Somehow don't get track info, ignore
      return null;
    }
    JsonBrowser firstColumn = columns.index(0)
            .get("musicResponsiveListItemFlexColumnRenderer")
            .get("text")
            .get("runs")
            .index(0);
    String title = firstColumn.get("text").text();
    String videoId = firstColumn.get("navigationEndpoint")
            .get("watchEndpoint")
            .get("videoId").text();
    if (videoId == null) {
      // If track is not available on YouTube Music videoId will be empty
      return null;
    }
    List<JsonBrowser> secondColumn = columns.index(1)
            .get("musicResponsiveListItemFlexColumnRenderer")
            .get("text")
            .get("runs").values();
    String author = secondColumn.get(0)
            .get("text").text();
    JsonBrowser lastElement = secondColumn.get(secondColumn.size() - 1);

    if (!lastElement.get("navigationEndpoint").isNull()) {
      // The duration element should not have this key, if it does, then duration is probably missing, so return
      return null;
    }

    long duration = DataFormatTools.durationTextToMillis(lastElement.get("text").text());

    AudioTrackInfo info = new AudioTrackInfo(title, author, duration, videoId, false,
        WATCH_URL_PREFIX + videoId);

    return trackFactory.apply(info);
  }
}