package com.sedmelluq.lavaplayer.core.source.youtube;

import com.sedmelluq.lavaplayer.core.http.HttpClientTools;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.info.playlist.AudioPlaylist;
import com.sedmelluq.lavaplayer.core.info.playlist.BasicAudioPlaylist;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoTemplate;
import com.sedmelluq.lavaplayer.core.tools.JsonBrowser;
import com.sedmelluq.lavaplayer.core.tools.Units;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.COMMON;

public class DefaultYoutubePlaylistLoader implements YoutubePlaylistLoader {
  private static final Logger log = LoggerFactory.getLogger(DefaultYoutubePlaylistLoader.class);

  private volatile int playlistPageCount = 6;

  @Override
  public void setPlaylistPageCount(int playlistPageCount) {
    this.playlistPageCount = playlistPageCount;
  }

  @Override
  public AudioPlaylist load(
      HttpInterface httpInterface,
      String playlistId,
      String selectedVideoId,
      AudioTrackInfoTemplate template
  ) {
    HttpGet playlistRequest = new HttpGet(getPlaylistUrl(playlistId) + "&pbj=1&hl=en");

    try (CloseableHttpResponse response = httpInterface.execute(playlistRequest)) {
      HttpClientTools.assertSuccessWithContent(response, "playlist response");
      HttpClientTools.assertJsonContentType(response);

      JsonBrowser json = JsonBrowser.parse(response.getEntity().getContent());
      return buildPlaylist(httpInterface, json, selectedVideoId, template);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private AudioPlaylist buildPlaylist(
      HttpInterface httpInterface,
      JsonBrowser json,
      String selectedVideoId,
      AudioTrackInfoTemplate template
  ) throws IOException {

    JsonBrowser jsonResponse = json.index(1).get("response");

    JsonBrowser alerts = jsonResponse.get("alerts");

    if (!alerts.isNull()) {
      throw new FriendlyException(alerts.index(0).get("alertRenderer").get("text").get("simpleText").text(), COMMON, null);
    }

    JsonBrowser info = jsonResponse
        .get("sidebar")
        .get("playlistSidebarRenderer")
        .get("items")
        .index(0)
        .get("playlistSidebarPrimaryInfoRenderer");

    String playlistName = info
        .get("title")
        .get("runs")
        .index(0)
        .get("text")
        .text();

    JsonBrowser playlistVideoList = jsonResponse
        .get("contents")
        .get("twoColumnBrowseResultsRenderer")
        .get("tabs")
        .index(0)
        .get("tabRenderer")
        .get("content")
        .get("sectionListRenderer")
        .get("contents")
        .index(0)
        .get("itemSectionRenderer")
        .get("contents")
        .index(0)
        .get("playlistVideoListRenderer")
        .get("contents");

    List<AudioTrackInfo> tracks = new ArrayList<>();
    String loadMoreUrl = extractPlaylistTracks(playlistVideoList, tracks, template);
    int loadCount = 0;
    int pageCount = playlistPageCount;

    // Also load the next pages, each result gives us a JSON with separate values for list html and next page loader html
    while (loadMoreUrl != null && ++loadCount < pageCount) {
      try (CloseableHttpResponse response = httpInterface.execute(new HttpGet("https://www.youtube.com" + loadMoreUrl))) {
        HttpClientTools.assertSuccessWithContent(response, "playlist response");

        JsonBrowser continuationJson = JsonBrowser.parse(response.getEntity().getContent());

        JsonBrowser playlistVideoListPage = continuationJson.index(1)
            .get("response")
            .get("continuationContents")
            .get("playlistVideoListContinuation");

        if (playlistVideoListPage.isNull()) {
          playlistVideoListPage = continuationJson.index(1)
              .get("response")
              .get("onResponseReceivedActions")
              .index(0)
              .get("appendContinuationItemsAction")
              .get("continuationItems");
        }

        loadMoreUrl = extractPlaylistTracks(playlistVideoListPage, tracks, template);
      }
    }

    return new BasicAudioPlaylist(playlistName, tracks, findSelectedTrack(tracks, selectedVideoId), false);
  }

  private AudioTrackInfo findSelectedTrack(List<AudioTrackInfo> tracks, String selectedVideoId) {
    if (selectedVideoId != null) {
      for (AudioTrackInfo track : tracks) {
        if (selectedVideoId.equals(track.getIdentifier())) {
          return track;
        }
      }
    }

    return null;
  }

  private String extractPlaylistTracks(
      JsonBrowser playlistVideoList,
      List<AudioTrackInfo> tracks,
      AudioTrackInfoTemplate template
  ) {
    if (playlistVideoList.isNull()) {
      return null;
    }

    List<JsonBrowser> playlistTrackEntries = playlistVideoList.values();

    for (JsonBrowser track : playlistTrackEntries) {
      JsonBrowser item = track.get("playlistVideoRenderer");

      JsonBrowser shortBylineText = item.get("shortBylineText");

      // If the isPlayable property does not exist, it means the video is removed or private
      // If the shortBylineText property does not exist, it means the Track is Region blocked
      if (!item.get("isPlayable").isNull() && !shortBylineText.isNull()) {
        String videoId = item.get("videoId").text();

        JsonBrowser titleField = item.get("title");
        String title = Optional.ofNullable(titleField.get("simpleText").text())
            .orElse(titleField.get("runs").index(0).get("text").text());

        String author = shortBylineText.get("runs").index(0).get("text").text();

        JsonBrowser lengthSeconds = item.get("lengthSeconds");
        long duration = Units.secondsToMillis(lengthSeconds.asLong(Units.DURATION_SEC_UNKNOWN));

        tracks.add(YoutubeTrackInfoFactory.create(template, videoId, author, title, duration, false));
      }
    }

    String continuationsToken = getContinuationsToken(playlistTrackEntries, playlistVideoList);

    if (continuationsToken != null && !continuationsToken.isEmpty()) {
      return "/browse_ajax?continuation=" + continuationsToken + "&ctoken=" + continuationsToken + "&hl=en";
    }

    return null;
  }

  private static String getContinuationsToken(List<JsonBrowser> playlistTrackEntries, JsonBrowser playlistVideoList) {
    JsonBrowser videoListContinuations = playlistVideoList.get("continuations");

    if (!videoListContinuations.isNull()) {
      return videoListContinuations.index(0).get("nextContinuationData").get("continuation").text();
    } else if (playlistTrackEntries.isEmpty()) {
      return null;
    }

    return playlistTrackEntries
        .get(playlistTrackEntries.size() - 1)
        .get("continuationItemRenderer")
        .get("continuationEndpoint")
        .get("continuationCommand")
        .get("token")
        .text();
  }

  private static String getPlaylistUrl(String playlistId) {
    return "https://www.youtube.com/playlist?list=" + playlistId;
  }
}
