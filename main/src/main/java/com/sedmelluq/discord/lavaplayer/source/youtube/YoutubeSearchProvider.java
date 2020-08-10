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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles processing YouTube searches.
 */
public class YoutubeSearchProvider implements YoutubeSearchResultLoader {
  private static final Logger log = LoggerFactory.getLogger(YoutubeSearchProvider.class);

  private static final String WATCH_URL_PREFIX = "https://www.youtube.com/watch?v=";
  private final HttpInterfaceManager httpInterfaceManager;
  private final Pattern polymerInitialDataRegex = Pattern.compile("window\\[\"ytInitialData\"]\\s*=\\s*(.*);+\\n");

  public YoutubeSearchProvider() {
    this.httpInterfaceManager = HttpClientTools.createCookielessThreadLocalManager();
  }

  public ExtendedHttpConfigurable getHttpConfiguration() {
    return httpInterfaceManager;
  }

  /**
   * @param query Search query.
   * @return Playlist of the first page of results.
   */
  @Override
  public AudioItem loadSearchResult(String query, Function<AudioTrackInfo, AudioTrack> trackFactory) {
    log.debug("Performing a search with query {}", query);

    try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
      URI url = new URIBuilder("https://www.youtube.com/results").addParameter("search_query", query).build();

      try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(url))) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (!HttpClientTools.isSuccessWithContent(statusCode)) {
          throw new IOException("Invalid status code for search response: " + statusCode);
        }

        Document document = Jsoup.parse(response.getEntity().getContent(), StandardCharsets.UTF_8.name(), "");
        return extractSearchResults(document, query, trackFactory);
      }
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions(e);
    }
  }

  private AudioItem extractSearchResults(Document document, String query,
                                         Function<AudioTrackInfo, AudioTrack> trackFactory) {

    List<AudioTrack> tracks = new ArrayList<>();
    Elements resultsSelection = document.select("#page > #content #results");
    if (!resultsSelection.isEmpty()) {
      for (Element results : resultsSelection) {
        for (Element result : results.select(".yt-lockup-video")) {
          if (!result.hasAttr("data-ad-impressions") && result.select(".standalone-ypc-badge-renderer-label").isEmpty()) {
            extractTrackFromResultEntry(tracks, result, trackFactory);
          }
        }
      }
    } else {
      log.debug("Attempting to parse results page as polymer");
      try {
        tracks = polymerExtractTracks(document, trackFactory);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    if (tracks.isEmpty()) {
      return AudioReference.NO_TRACK;
    } else {
      return new BasicAudioPlaylist("Search results for: " + query, tracks, null, true);
    }
  }

  private void extractTrackFromResultEntry(List<AudioTrack> tracks, Element element,
                                           Function<AudioTrackInfo, AudioTrack> trackFactory) {

    Element durationElement = element.select("[class^=video-time]").first();
    Element contentElement = element.select(".yt-lockup-content").first();
    String videoId = element.attr("data-context-item-id");

    if (durationElement == null || contentElement == null || videoId.isEmpty()) {
      return;
    }

    long duration = DataFormatTools.durationTextToMillis(durationElement.text());

    String title = contentElement.select(".yt-lockup-title > a").text();
    String author = contentElement.select(".yt-lockup-byline > a").text();

    AudioTrackInfo info = new AudioTrackInfo(title, author, duration, videoId, false,
        WATCH_URL_PREFIX + videoId);

    tracks.add(trackFactory.apply(info));
  }

  private List<AudioTrack> polymerExtractTracks(Document document, Function<AudioTrackInfo, AudioTrack> trackFactory) throws IOException {
    // Match the JSON from the HTML. It should be within a script tag
    Matcher matcher = polymerInitialDataRegex.matcher(document.outerHtml());
    if (!matcher.find()) {
      log.warn("Failed to match ytInitialData JSON object");
      return Collections.emptyList();
    }

    JsonBrowser jsonBrowser = JsonBrowser.parse(matcher.group(1));
    ArrayList<AudioTrack> list = new ArrayList<>();
    jsonBrowser.get("contents")
        .get("twoColumnSearchResultsRenderer")
        .get("primaryContents")
        .get("sectionListRenderer")
        .get("contents")
        .index(0)
        .get("itemSectionRenderer")
        .get("contents")
        .values()
        .forEach(json -> {
          AudioTrack track = extractPolymerData(json, trackFactory);
          if (track != null) list.add(track);
        });
    return list;
  }

  private AudioTrack extractPolymerData(JsonBrowser json, Function<AudioTrackInfo, AudioTrack> trackFactory) {
    json = json.get("videoRenderer");
    if (json.isNull()) return null; // Ignore everything which is not a track

    String title = json.get("title").get("runs").index(0).get("text").text();
    String author = json.get("ownerText").get("runs").index(0).get("text").text();
    if (json.get("lengthText").isNull()) {
      return null; // Ignore if the video is a live stream
    }
    long duration = DataFormatTools.durationTextToMillis(json.get("lengthText").get("simpleText").text());
    String videoId = json.get("videoId").text();

    AudioTrackInfo info = new AudioTrackInfo(title, author, duration, videoId, false,
        WATCH_URL_PREFIX + videoId);

    return trackFactory.apply(info);
  }
}
