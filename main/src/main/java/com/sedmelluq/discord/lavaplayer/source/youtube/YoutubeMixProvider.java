package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.lava.common.tools.DaemonThreadFactory;
import com.sedmelluq.lava.common.tools.ExecutorTools;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Handles loading of YouTube mixes.
 */
public class YoutubeMixProvider {
  private static final Logger log = LoggerFactory.getLogger(YoutubeMixProvider.class);

  private static final int MIX_QUEUE_CAPACITY = 5000;

  private final YoutubeAudioSourceManager sourceManager;
  private final ThreadPoolExecutor mixLoadingExecutor;

  /**
   * @param sourceManager YouTube source manager used for created tracks.
   */
  public YoutubeMixProvider(YoutubeAudioSourceManager sourceManager) {
    this.sourceManager = sourceManager;
    mixLoadingExecutor = new ThreadPoolExecutor(0, 10, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<>(MIX_QUEUE_CAPACITY),
        new DaemonThreadFactory("yt-mix"));
  }

  /**
   * @param maximumPoolSize Maximum number of threads in mix loader thread pool.
   */
  public void setLoaderMaximumPoolSize(int maximumPoolSize) {
    mixLoadingExecutor.setMaximumPoolSize(maximumPoolSize);
  }

  /**
   * Shuts down mix loading threads.
   */
  public void shutdown() {
    ExecutorTools.shutdownExecutor(mixLoadingExecutor, "youtube mix");
  }

  /**
   * Loads tracks from mix in parallel into a playlist entry.
   *
   * @param mixId ID of the mix
   * @param selectedVideoId Selected track, {@link AudioPlaylist#getSelectedTrack()} will return this.
   * @return Playlist of the tracks in the mix.
   */
  public AudioItem loadMixWithId(String mixId, String selectedVideoId) {
    String playlistTitle = "YouTube mix";
    List<AudioTrack> tracks = new ArrayList<>();

    try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
      String mixUrl = "https://www.youtube.com/watch?v=" + selectedVideoId + "&list=" + mixId + "&pbj=1";

      try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(mixUrl))) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (!HttpClientTools.isSuccessWithContent(statusCode)) {
          throw new IOException("Invalid status code for mix response: " + statusCode);
        }

        JsonBrowser body = JsonBrowser.parse(response.getEntity().getContent());
        JsonBrowser playlist = body.index(3).get("response")
                .get("contents")
                .get("twoColumnWatchNextResults")
                .get("playlist")
                .get("playlist");

        JsonBrowser title = playlist.get("title");

        if (!title.isNull()) {
          playlistTitle = title.text();
        }

        extractPlaylistTracks(playlist.get("contents"), tracks);
      }
    } catch (IOException e) {
      throw new FriendlyException("Could not read mix page.", SUSPICIOUS, e);
    }

    if (tracks.isEmpty()) {
      throw new FriendlyException("Could not find tracks from mix.", SUSPICIOUS, null);
    }

    AudioTrack selectedTrack = findSelectedTrack(tracks, selectedVideoId);
    return new BasicAudioPlaylist(playlistTitle, tracks, selectedTrack, false);
  }

  private void extractPlaylistTracks(JsonBrowser browser, List<AudioTrack> tracks) {
    for (JsonBrowser video : browser.values()) {
      JsonBrowser renderer = video.get("playlistPanelVideoRenderer");
      String title = renderer.get("title").get("simpleText").text();
      String author = renderer.get("longBylineText").get("runs").index(0).get("text").text();
      String durationStr = renderer.get("lengthText").get("simpleText").text();
      long duration = parseDuration(durationStr);
      String identifier = renderer.get("videoId").text();
      String uri = "https://youtube.com/watch?v=" + identifier;

      AudioTrackInfo trackInfo = new AudioTrackInfo(title, author, duration, identifier, false, uri);
      tracks.add(new YoutubeAudioTrack(trackInfo, sourceManager));
    }
  }

  private long parseDuration(String duration) {
    String[] parts = duration.split(":");

    if (parts.length == 3) { // hh::mm:ss
      int hours = Integer.parseInt(parts[0]);
      int minutes = Integer.parseInt(parts[1]);
      int seconds = Integer.parseInt(parts[2]);
      return (hours * 3600000) + (minutes * 60000) + (seconds * 1000);
    } else if (parts.length == 2) { // mm:ss
      int minutes = Integer.parseInt(parts[0]);
      int seconds = Integer.parseInt(parts[1]);
      return (minutes * 60000) + (seconds * 1000);
    } else {
      return -1L;
    }
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
}
