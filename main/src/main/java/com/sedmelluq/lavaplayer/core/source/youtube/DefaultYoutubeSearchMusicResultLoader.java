package com.sedmelluq.lavaplayer.core.source.youtube;

import com.sedmelluq.lavaplayer.core.http.ExtendedHttpConfigurable;
import com.sedmelluq.lavaplayer.core.http.HttpClientTools;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.HttpInterfaceManager;
import com.sedmelluq.lavaplayer.core.info.AudioInfoEntity;
import com.sedmelluq.lavaplayer.core.info.playlist.BasicAudioPlaylist;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoTemplate;
import com.sedmelluq.lavaplayer.core.tools.DataFormatTools;
import com.sedmelluq.lavaplayer.core.tools.JsonBrowser;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

public class DefaultYoutubeSearchMusicResultLoader implements YoutubeSearchMusicResultLoader {
  private static final Logger log = LoggerFactory.getLogger(DefaultYoutubeSearchMusicResultLoader.class);

  private static final String YT_MUSIC_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30";
  private static final String YT_MUSIC_PAYLOAD = "{\"context\":{\"client\":{\"clientName\":\"WEB_REMIX\",\"clientVersion\":\"0.1\"}},\"query\":\"%s\",\"params\":\"Eg-KAQwIARAAGAAgACgAMABqChADEAQQCRAFEAo=\"}";
  private final HttpInterfaceManager httpInterfaceManager;
  private final Pattern ytMusicDataRegex = Pattern.compile("<body>\\s*(.*)\\s*</body>");

  public DefaultYoutubeSearchMusicResultLoader() {
    this.httpInterfaceManager = HttpClientTools.createCookielessThreadLocalManager();
  }

  public ExtendedHttpConfigurable getHttpConfiguration() {
    return httpInterfaceManager;
  }

  /**
   * @param query Search query.
   * @return Playlist of the first page of music results.
   */
  @Override
  public AudioInfoEntity loadSearchMusicResult(String query, AudioTrackInfoTemplate template) {
    log.debug("Performing a search music with query {}", query);

    try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
      URI url = new URIBuilder("https://music.youtube.com/youtubei/v1/search")
          .addParameter("alt", "json")
          .addParameter("key", YT_MUSIC_KEY).build();

      HttpPost post = new HttpPost(url);
      StringEntity payload = new StringEntity(String.format(YT_MUSIC_PAYLOAD, query), "UTF-8");
      post.setHeader("Referer", "music.youtube.com");
      post.setEntity(payload);
      try (CloseableHttpResponse response = httpInterface.execute(post)) {
        HttpClientTools.assertSuccessWithContent(response, "search music response");

        Document document = Jsoup.parse(response.getEntity().getContent(), StandardCharsets.UTF_8.name(), "");
        return extractSearchResults(document, query, template);
      }
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions(e);
    }
  }

  private AudioInfoEntity extractSearchResults(Document document, String query, AudioTrackInfoTemplate template) {
    List<AudioTrackInfo> tracks;
    try {
      tracks = extractMusicTracks(document, template);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (tracks.isEmpty()) {
      return AudioInfoEntity.NO_INFO;
    } else {
      return new BasicAudioPlaylist("Search music results for: " + query, tracks, null, true);
    }
  }

  private List<AudioTrackInfo> extractMusicTracks(Document document, AudioTrackInfoTemplate template) throws IOException {
    Matcher matcher = ytMusicDataRegex.matcher(document.outerHtml());
    if (!matcher.find()) {
      log.warn("Failed to match ytMusicData JSON object");
      return Collections.emptyList();
    }

    JsonBrowser jsonBrowser = JsonBrowser.parse(matcher.group(1));
    List<AudioTrackInfo> list = new ArrayList<>();
    JsonBrowser tracks = jsonBrowser.get("contents")
        .get("sectionListRenderer")
        .get("contents")
        .index(0)
        .get("musicShelfRenderer")
        .get("contents");
    if (tracks == JsonBrowser.NULL_BROWSER) {
      tracks = jsonBrowser.get("contents")
          .get("sectionListRenderer")
          .get("contents")
          .index(1)
          .get("musicShelfRenderer")
          .get("contents");
    }
    tracks.values().forEach(json -> {
      AudioTrackInfo track = extractMusicData(json, template);

      if (track != null) {
        list.add(track);
      }
    });
    return list;
  }

  private AudioTrackInfo extractMusicData(JsonBrowser json, AudioTrackInfoTemplate template) {
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
    List<JsonBrowser> secondColumn = columns.index(1)
        .get("musicResponsiveListItemFlexColumnRenderer")
        .get("text")
        .get("runs").values();
    String author = secondColumn.get(0)
        .get("text").text();
    long duration = DataFormatTools.durationTextToMillis(secondColumn.get(secondColumn.size() - 1)
        .get("text").text());

    return YoutubeTrackInfoFactory.create(template, videoId, author, title, duration, false);
  }
}
