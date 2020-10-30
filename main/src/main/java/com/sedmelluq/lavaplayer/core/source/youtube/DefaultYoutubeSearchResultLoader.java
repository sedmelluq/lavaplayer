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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles processing YouTube searches.
 */
public class DefaultYoutubeSearchResultLoader implements YoutubeSearchResultLoader {
  private static final Logger log = LoggerFactory.getLogger(DefaultYoutubeSearchResultLoader.class);

  private final HttpInterfaceManager httpInterfaceManager;
  private final Pattern polymerInitialDataRegex = Pattern.compile("(window\\[\"ytInitialData\"]|var ytInitialData)\\s*=\\s*(.*);");

  public DefaultYoutubeSearchResultLoader() {
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
  public AudioInfoEntity loadSearchResult(String query, AudioTrackInfoTemplate template) {
    log.debug("Performing a search with query {}", query);

    try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
      URI url = new URIBuilder("https://www.youtube.com/results").addParameter("search_query", query).build();

      try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(url))) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (!HttpClientTools.isSuccessWithContent(statusCode)) {
          throw new IOException("Invalid status code for search response: " + statusCode);
        }

        Document document = Jsoup.parse(response.getEntity().getContent(), StandardCharsets.UTF_8.name(), "");
        return extractSearchResults(document, query, template);
      }
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions(e);
    }
  }

  private AudioInfoEntity extractSearchResults(Document document, String query, AudioTrackInfoTemplate template) {
    List<AudioTrackInfo> tracks = new ArrayList<>();

    Elements resultsSelection = document.select("#page > #content #results");
    if (!resultsSelection.isEmpty()) {
      for (Element results : resultsSelection) {
        for (Element result : results.select(".yt-lockup-video")) {
          if (!result.hasAttr("data-ad-impressions") && result.select(".standalone-ypc-badge-renderer-label").isEmpty()) {
            extractTrackFromResultEntry(tracks, result, template);
          }
        }
      }
    } else {
      log.debug("Attempting to parse results page as polymer");

      try {
        tracks = polymerExtractTracks(document, template);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    if (tracks.isEmpty()) {
      return AudioInfoEntity.NO_INFO;
    } else {
      return new BasicAudioPlaylist("Search results for: " + query, tracks, null, true);
    }
  }

  private void extractTrackFromResultEntry(List<AudioTrackInfo> tracks, Element element, AudioTrackInfoTemplate template) {
    Element durationElement = element.select("[class^=video-time]").first();
    Element contentElement = element.select(".yt-lockup-content").first();
    String videoId = element.attr("data-context-item-id");

    if (durationElement == null || contentElement == null || videoId.isEmpty()) {
      return;
    }

    long duration = DataFormatTools.durationTextToMillis(durationElement.text());

    String title = contentElement.select(".yt-lockup-title > a").text();
    String author = contentElement.select(".yt-lockup-byline > a").text();

    tracks.add(YoutubeTrackInfoFactory.create(template, videoId, author, title, duration, false));
  }

  private List<AudioTrackInfo> polymerExtractTracks(Document document, AudioTrackInfoTemplate template) throws IOException {
    // Match the JSON from the HTML. It should be within a script tag
    Matcher matcher = polymerInitialDataRegex.matcher(document.outerHtml());
    if (!matcher.find()) {
      log.warn("Failed to match ytInitialData JSON object");
      return Collections.emptyList();
    }

    JsonBrowser jsonBrowser = JsonBrowser.parse(matcher.group(2));
    ArrayList<AudioTrackInfo> list = new ArrayList<>();

    List<JsonBrowser> trackHolders = jsonBrowser.get("contents")
        .get("twoColumnSearchResultsRenderer")
        .get("primaryContents")
        .get("sectionListRenderer")
        .get("contents")
        .index(0)
        .get("itemSectionRenderer")
        .get("contents")
        .values();

    for (JsonBrowser holder : trackHolders) {
      AudioTrackInfo track = extractPolymerData(holder, template);

      if (track != null) {
        list.add(track);
      }
    }

    return list;
  }

  private AudioTrackInfo extractPolymerData(JsonBrowser json, AudioTrackInfoTemplate template) {
    JsonBrowser renderer = json.get("videoRenderer");

    if (renderer.isNull()) {
      // Not a track, ignore
      return null;
    }

    String title = renderer.get("title").get("runs").index(0).get("text").text();
    String author = renderer.get("ownerText").get("runs").index(0).get("text").text();
    String lengthText = renderer.get("lengthText").get("simpleText").text();

    if (lengthText == null) {
      // Unknown length means this is a livestream, ignore
      return null;
    }

    long duration = DataFormatTools.durationTextToMillis(lengthText);
    String videoId = renderer.get("videoId").text();

    return YoutubeTrackInfoFactory.create(template, videoId, author, title, duration, false);
  }
}
