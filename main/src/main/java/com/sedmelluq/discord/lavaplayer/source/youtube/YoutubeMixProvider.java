package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.DaemonThreadFactory;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.ExecutorTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
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
    List<String> videoIds = new ArrayList<>();

    try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
      String mixUrl = "https://www.youtube.com/watch?v=" + selectedVideoId + "&list=" + mixId;

      try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(mixUrl))) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
          throw new IOException("Invalid status code for mix response: " + statusCode);
        }

        Document document = Jsoup.parse(response.getEntity().getContent(), StandardCharsets.UTF_8.name(), "");
        extractVideoIdsFromMix(document, videoIds);

        if (videoIds.isEmpty() && !document.select("#player-unavailable").isEmpty()) {
          return AudioReference.NO_TRACK;
        }
      }
    } catch (IOException e) {
      throw new FriendlyException("Could not read mix page.", SUSPICIOUS, e);
    }

    if (videoIds.isEmpty()) {
      throw new FriendlyException("Could not find tracks from mix.", SUSPICIOUS, null);
    }

    return loadTracksAsynchronously(videoIds, selectedVideoId);
  }

  private void extractVideoIdsFromMix(Document document, List<String> videoIds) {
    for (Element videoList : document.select("#playlist-autoscroll-list")) {
      for (Element item : videoList.select("li")) {
        videoIds.add(item.attr("data-video-id"));
      }
    }
  }

  private AudioPlaylist loadTracksAsynchronously(List<String> videoIds, String selectedVideoId) {
    ExecutorCompletionService<AudioItem> completion = new ExecutorCompletionService<>(mixLoadingExecutor);
    List<AudioTrack> tracks = new ArrayList<>();

    for (final String videoId : videoIds) {
      completion.submit(() -> sourceManager.loadTrackWithVideoId(videoId, true));
    }

    try {
      fetchTrackResultsFromExecutor(completion, tracks, videoIds.size());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    AudioTrack selectedTrack = sourceManager.findSelectedTrack(tracks, selectedVideoId);

    if (tracks.isEmpty()) {
      throw new FriendlyException("No tracks from the mix loaded succesfully.", SUSPICIOUS, null);
    } else if (selectedTrack == null) {
      throw new FriendlyException("The selected track of the mix failed to load.", SUSPICIOUS, null);
    }

    return new BasicAudioPlaylist("YouTube mix", tracks, selectedTrack, false);
  }

  private void fetchTrackResultsFromExecutor(ExecutorCompletionService<AudioItem> completion, List<AudioTrack> tracks, int size) throws InterruptedException {
    for (int i = 0; i < size; i++) {
      try {
        AudioItem item = completion.take().get();

        if (item instanceof AudioTrack) {
          tracks.add((AudioTrack) item);
        }
      } catch (ExecutionException e) {
        if (e.getCause() instanceof FriendlyException) {
          ExceptionTools.log(log, (FriendlyException) e.getCause(), "Loading a track from a mix.");
        } else {
          log.warn("Failed to load a track from a mix.", e);
        }
      }
    }
  }
}
