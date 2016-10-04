package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioTrackExecutor;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

  private static final String VIDEO_ID_REGEX = "([a-zA-Z0-9_-]{11})";
  private static final String PLAYLIST_REGEX = "(PL[a-zA-Z0-9]+)";
  private static final String PROTOCOL_REGEX = "(?:http://|https://|)";
  private static final String SUFFIX_REGEX = "(?:\\?.*|&.*|)";

  private static final Pattern[] validTrackPatterns = new Pattern[] {
      Pattern.compile("^" + VIDEO_ID_REGEX + "$"),
      Pattern.compile("^" + PROTOCOL_REGEX + "(?:www\\.|)youtube.com/watch\\?v=" + VIDEO_ID_REGEX + SUFFIX_REGEX + "$"),
      Pattern.compile("^" + PROTOCOL_REGEX + "(?:www\\.|)youtu.be/" + VIDEO_ID_REGEX + SUFFIX_REGEX + "$")
  };

  private static final Pattern[] validPlaylistPatterns = new Pattern[] {
      Pattern.compile("^" + PLAYLIST_REGEX + "$"),
      Pattern.compile("^" + PROTOCOL_REGEX + "(?:www\\.|)youtube.com/playlist\\?list=" + PLAYLIST_REGEX + SUFFIX_REGEX + "$")
  };

  private final HttpClientBuilder httpClientBuilder;
  private final YoutubeSignatureCipherManager signatureCipherManager;

  /**
   * Create an instance.
   */
  public YoutubeAudioSourceManager() {
    httpClientBuilder = createSharedCookiesHttpBuilder();
    signatureCipherManager = new YoutubeSignatureCipherManager();
  }

  /**
   * @return New HttpClient instance.
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

  @Override
  public AudioItem loadItem(String identifier) {
    AudioItem result;

    if ((result = loadTrack(identifier)) == null) {
      result = loadPlaylist(identifier);
    }

    return result;
  }

  private AudioTrack loadTrack(String identifier) {
    for (Pattern pattern : validTrackPatterns) {
      Matcher matcher = pattern.matcher(identifier);

      if (matcher.matches()) {
        return loadTrackWithVideoId(matcher.group(1));
      }
    }

    return null;
  }

  private AudioTrack loadTrackWithVideoId(String videoId) {
    try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
      JsonBrowser info = getTrackInfoFromMainPage(httpClient, videoId, false);
      if (info == null) {
        return null;
      }

      JsonBrowser args = info.get("args");
      AudioTrackInfo trackInfo = new AudioTrackInfo(
          args.get("title").text(), args.get("author").text(), args.get("length_seconds").as(Integer.class)
      );

      return new YoutubeAudioTrack(new AudioTrackExecutor(videoId), trackInfo, this);
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions("Loading information for a YouTube track failed.", FAULT, e);
    }
  }

  JsonBrowser getTrackInfoFromMainPage(CloseableHttpClient httpClient, String videoId, boolean mustExist) throws Exception {
    try (CloseableHttpResponse response = httpClient.execute(new HttpGet("https://www.youtube.com/watch?v=" + videoId))) {
      if (response.getStatusLine().getStatusCode() != 200) {
        throw new IOException("Invalid status code for video page response.");
      }

      String html = IOUtils.toString(response.getEntity().getContent(), Charset.forName("UTF-8"));
      String configJson = DataFormatTools.extractBetween(html, "ytplayer.config = ", ";ytplayer.load");

      if (configJson != null) {
        return JsonBrowser.parse(configJson);
      }
    }

    determineFailureReason(httpClient, videoId, mustExist);
    return null;
  }

  private void determineFailureReason(CloseableHttpClient httpClient, String videoId, boolean mustExist) throws Exception {
    try (CloseableHttpResponse response = httpClient.execute(new HttpGet("https://www.youtube.com/get_video_info?hl=en_GB&video_id=" + videoId))) {
      if (response.getStatusLine().getStatusCode() != 200) {
        throw new IOException("Invalid status code for video info response.");
      }

      Map<String, String> format = convertToMapLayout(URLEncodedUtils.parse(response.getEntity()));

      if ("fail".equals(format.get("status"))) {
        String reason = format.get("reason");

        if ("This video does not exist.".equals(reason) && !mustExist) {
          return;
        } else if (reason != null) {
          throw new FriendlyException(reason, COMMON, null);
        }
      }

      throw new FriendlyException("Track is unavailable for an unknown reason.", SUSPICIOUS,
          new IllegalStateException("Main page had no video, but video info has no error."));
    }
  }

  private AudioPlaylist loadPlaylist(String identifier) {
    for (Pattern pattern : validPlaylistPatterns) {
      Matcher matcher = pattern.matcher(identifier);

      if (matcher.matches()) {
        return loadPlaylistWithId(matcher.group(1));
      }
    }

    return null;
  }

  private AudioPlaylist loadPlaylistWithId(String playlistId) {
    log.debug("Starting to load playlist with ID {}", playlistId);

    try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
      try (CloseableHttpResponse response = httpClient.execute(new HttpGet("https://www.youtube.com/playlist?list=" + playlistId))) {
        if (response.getStatusLine().getStatusCode() != 200) {
          throw new IOException("Invalid status code for playlist response.");
        }

        Document document = Jsoup.parse(response.getEntity().getContent(), "UTF-8", "");
        return buildPlaylist(httpClient, document);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private AudioPlaylist buildPlaylist(CloseableHttpClient httpClient, Document document) throws IOException {
    Element container = document.select("#pl-header").get(0).parent();

    String playlistName = container.select(".pl-header-title").get(0).text();

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

    return new BasicAudioPlaylist(playlistName, tracks);
  }

  private String extractPlaylistTracks(Element videoContainer, Element loadMoreContainer, List<AudioTrack> tracks) {
    for (Element video : videoContainer.select(".pl-video")) {
      Elements lengthElements = video.select(".timestamp span");

      // If the timestamp element does not exist, it means the video is private
      if (!lengthElements.isEmpty()) {
        String videoId = video.attr("data-video-id").trim();
        String title = video.attr("data-title").trim();
        String author = video.select(".pl-video-owner a").text().trim();

        int lengthInSeconds = lengthTextToSeconds(lengthElements.get(0).text());

        AudioTrackInfo info = new AudioTrackInfo(title, author, lengthInSeconds);
        tracks.add(new YoutubeAudioTrack(new AudioTrackExecutor(videoId), info, this));
      }
    }

    if (loadMoreContainer != null) {
      Elements more = loadMoreContainer.select(".load-more-button");
      if (!more.isEmpty()) {
        return more.get(0).attr("data-uix-load-more-href");
      }
    }

    return null;
  }

  private int lengthTextToSeconds(String durationText) {
    String[] parts = durationText.split(":");
    return Integer.valueOf(parts[0]) * 60 + Integer.valueOf(parts[1]);
  }
}
