package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Handles processing YouTube searches.
 */
public class YoutubeSearchProvider implements HttpConfigurable {
  private static final Logger log = LoggerFactory.getLogger(YoutubeSearchProvider.class);

  private final YoutubeAudioSourceManager sourceManager;
  private final HttpInterfaceManager httpInterfaceManager;

  /**
   * @param sourceManager YouTube source manager used for created tracks.
   */
  public YoutubeSearchProvider(YoutubeAudioSourceManager sourceManager) {
    this.sourceManager = sourceManager;
    this.httpInterfaceManager = HttpClientTools.createCookielessThreadLocalManager();
  }

  /**
   * @param query Search query.
   * @return Playlist of the first page of results.
   */
  public AudioItem loadSearchResult(String query) {
    log.debug("Performing a search with query {}", query);

    try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
      URI url = new URIBuilder("https://www.youtube.com/results").addParameter("search_query", query).build();

      try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(url))) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
          throw new IOException("Invalid status code for search response: " + statusCode);
        }

        Document document = Jsoup.parse(response.getEntity().getContent(), StandardCharsets.UTF_8.name(), "");
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
        if (!result.hasAttr("data-ad-impressions") && result.select(".standalone-ypc-badge-renderer-label").isEmpty()) {
          extractTrackFromResultEntry(tracks, result);
        }
      }
    }

    if (tracks.isEmpty()) {
      return AudioReference.NO_TRACK;
    } else {
      return new BasicAudioPlaylist("Search results for: " + query, tracks, null, true);
    }
  }

  private void extractTrackFromResultEntry(List<AudioTrack> tracks, Element element) {
    Element durationElement = element.select("[class^=video-time]").first();
    Element contentElement = element.select(".yt-lockup-content").first();
    String videoId = element.attr("data-context-item-id");

    if (durationElement == null || contentElement == null || videoId.isEmpty()) {
      return;
    }

    long duration = DataFormatTools.durationTextToMillis(durationElement.text());

    String title = contentElement.select(".yt-lockup-title > a").text();
    String author = contentElement.select(".yt-lockup-byline > a").text();

    tracks.add(sourceManager.buildTrackObject(videoId, title, author, false, duration));
  }

  @Override
  public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
    httpInterfaceManager.configureRequests(configurator);
  }

  @Override
  public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
    httpInterfaceManager.configureBuilder(configurator);
  }
}
