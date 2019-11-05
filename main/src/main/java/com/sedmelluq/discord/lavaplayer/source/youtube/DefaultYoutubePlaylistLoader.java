package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;

public class DefaultYoutubePlaylistLoader implements YoutubePlaylistLoader {
  private volatile int playlistPageCount = 6;

  @Override
  public void setPlaylistPageCount(int playlistPageCount) {
    this.playlistPageCount = playlistPageCount;
  }

  @Override
  public AudioPlaylist load(HttpInterface httpInterface, String playlistId, String selectedVideoId,
                            Function<AudioTrackInfo, AudioTrack> trackFactory) {

    HttpGet request = new HttpGet(getPlaylistUrl(playlistId) + "&pbj=1&hl=en");

    try (CloseableHttpResponse response = httpInterface.execute(request)) {
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != 200) {
        throw new IOException("Invalid status code for playlist response: " + statusCode);
      }

      JsonBrowser json = JsonBrowser.parse(response.getEntity().getContent());

      return buildPlaylist(httpInterface, json, selectedVideoId, trackFactory);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private AudioPlaylist buildPlaylist(HttpInterface httpInterface, JsonBrowser json, String selectedVideoId,
                                      Function<AudioTrackInfo, AudioTrack> trackFactory) throws IOException {

    JsonBrowser jsonResponse = json.index(1).safeGet("response");

    JsonBrowser alerts = jsonResponse.safeGet("alerts");

    if (!alerts.isNull()) throw new FriendlyException(alerts.index(0).safeGet("alertRenderer").safeGet("text").safeGet("simpleText").text(), COMMON, null);

    JsonBrowser info = jsonResponse
        .safeGet("sidebar")
        .safeGet("playlistSidebarRenderer")
        .safeGet("items")
        .index(0)
        .safeGet("playlistSidebarPrimaryInfoRenderer");

    String playlistName = info
        .safeGet("title")
        .safeGet("runs")
        .index(0)
        .safeGet("text")
        .text();

    JsonBrowser playlistVideoList = jsonResponse
        .safeGet("contents")
        .safeGet("twoColumnBrowseResultsRenderer")
        .safeGet("tabs")
        .index(0)
        .safeGet("tabRenderer")
        .safeGet("content")
        .safeGet("sectionListRenderer")
        .safeGet("contents")
        .index(0)
        .safeGet("itemSectionRenderer")
        .safeGet("contents")
        .index(0)
        .safeGet("playlistVideoListRenderer");

    List<AudioTrack> tracks = new ArrayList<>();
    String loadMoreUrl = extractPlaylistTracks(playlistVideoList, tracks, trackFactory);
    int loadCount = 0;
    int pageCount = playlistPageCount;

    // Also load the next pages, each result gives us a JSON with separate values for list html and next page loader html
    while (loadMoreUrl != null && ++loadCount < pageCount) {
      try (CloseableHttpResponse response = httpInterface.execute(new HttpGet("https://www.youtube.com" + loadMoreUrl))) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
          throw new IOException("Invalid status code for playlist response: " + statusCode);
        }

        JsonBrowser continuationJson = JsonBrowser.parse(response.getEntity().getContent());

        JsonBrowser playlistVideoListPage = continuationJson.index(1)
            .safeGet("response")
            .safeGet("continuationContents")
            .safeGet("playlistVideoListContinuation");

        loadMoreUrl = extractPlaylistTracks(playlistVideoListPage, tracks, trackFactory);
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

  private String extractPlaylistTracks(JsonBrowser playlistVideoList, List<AudioTrack> tracks,
                                       Function<AudioTrackInfo, AudioTrack> trackFactory) {

    JsonBrowser trackArray = playlistVideoList.safeGet("contents");

    if (trackArray.isNull()) return null;

    for (JsonBrowser track : trackArray.values()) {
      JsonBrowser item = track.safeGet("playlistVideoRenderer");

      JsonBrowser shortBylineText = item.safeGet("shortBylineText");

      // If the isPlayable property does not exist, it means the video is removed or private
      // If the shortBylineText property does not exist, it means the Track is Region blocked
      if (!item.safeGet("isPlayable").isNull() && !shortBylineText.isNull()) {
        String videoId = item.safeGet("videoId").text();
        String title = item.safeGet("title").safeGet("simpleText").text();
        String author = shortBylineText.safeGet("runs").index(0).safeGet("text").text();
        long duration = Long.parseLong(item.safeGet("lengthSeconds").text()) * 1000;

        AudioTrackInfo info = new AudioTrackInfo(title, author, duration, videoId, false,
            "https://www.youtube.com/watch?v=" + videoId,
            Collections.singletonMap("artworkUrl", String.format("https://img.youtube.com/vi/%s/0.jpg", videoId)));

        tracks.add(trackFactory.apply(info));
      }
    }

    JsonBrowser continuations = playlistVideoList.safeGet("continuations");

    if (!continuations.isNull()) {
      String continuationsToken = continuations.index(0).safeGet("nextContinuationData").safeGet("continuation").text();
      return "/browse_ajax" + "?continuation=" + continuationsToken + "&ctoken=" + continuationsToken + "&hl=en";
    }

    return null;
  }

  private static String getPlaylistUrl(String playlistId) {
    return "https://www.youtube.com/playlist?list=" + playlistId;
  }
}
