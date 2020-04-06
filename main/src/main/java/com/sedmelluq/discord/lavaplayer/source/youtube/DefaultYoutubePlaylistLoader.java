package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import java.io.IOException;
import java.util.ArrayList;
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
      if (!HttpClientTools.isSuccessWithContent(statusCode)) {
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
        .get("playlistVideoListRenderer");

    List<AudioTrack> tracks = new ArrayList<>();
    String loadMoreUrl = extractPlaylistTracks(playlistVideoList, tracks, trackFactory);
    int loadCount = 0;
    int pageCount = playlistPageCount;

    // Also load the next pages, each result gives us a JSON with separate values for list html and next page loader html
    while (loadMoreUrl != null && ++loadCount < pageCount) {
      try (CloseableHttpResponse response = httpInterface.execute(new HttpGet("https://www.youtube.com" + loadMoreUrl))) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (!HttpClientTools.isSuccessWithContent(statusCode)) {
          throw new IOException("Invalid status code for playlist response: " + statusCode);
        }

        JsonBrowser continuationJson = JsonBrowser.parse(response.getEntity().getContent());

        JsonBrowser playlistVideoListPage = continuationJson.index(1)
            .get("response")
            .get("continuationContents")
            .get("playlistVideoListContinuation");

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

    JsonBrowser trackArray = playlistVideoList.get("contents");

    if (trackArray.isNull()) return null;

    for (JsonBrowser track : trackArray.values()) {
      JsonBrowser item = track.get("playlistVideoRenderer");

      JsonBrowser shortBylineText = item.get("shortBylineText");

      // If the isPlayable property does not exist, it means the video is removed or private
      // If the shortBylineText property does not exist, it means the Track is Region blocked
      if (!item.get("isPlayable").isNull() && !shortBylineText.isNull()) {
        String videoId = item.get("videoId").text();
        String title = item.get("title").get("simpleText").text();
        String author = shortBylineText.get("runs").index(0).get("text").text();
        long duration = Long.parseLong(item.get("lengthSeconds").text()) * 1000;

        AudioTrackInfo info = new AudioTrackInfo(title, author, duration, videoId, false,
            "https://www.youtube.com/watch?v=" + videoId);

        tracks.add(trackFactory.apply(info));
      }
    }

    JsonBrowser continuations = playlistVideoList.get("continuations");

    if (!continuations.isNull()) {
      String continuationsToken = continuations.index(0).get("nextContinuationData").get("continuation").text();
      return "/browse_ajax?continuation=" + continuationsToken + "&ctoken=" + continuationsToken + "&hl=en";
    }

    return null;
  }

  private static String getPlaylistUrl(String playlistId) {
    return "https://www.youtube.com/playlist?list=" + playlistId;
  }
}
