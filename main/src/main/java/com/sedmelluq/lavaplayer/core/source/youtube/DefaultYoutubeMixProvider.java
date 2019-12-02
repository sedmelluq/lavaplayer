package com.sedmelluq.lavaplayer.core.source.youtube;

import com.sedmelluq.lava.common.tools.DaemonThreadFactory;
import com.sedmelluq.lava.common.tools.ExecutorTools;
import com.sedmelluq.lavaplayer.core.http.HttpClientTools;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.HttpInterfaceManager;
import com.sedmelluq.lavaplayer.core.info.AudioInfoEntity;
import com.sedmelluq.lavaplayer.core.info.playlist.AudioPlaylist;
import com.sedmelluq.lavaplayer.core.info.playlist.BasicAudioPlaylist;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoTemplate;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.SUSPICIOUS;

/**
 * Handles loading of YouTube mixes.
 */
public class DefaultYoutubeMixProvider implements YoutubeMixProvider {
  private static final Logger log = LoggerFactory.getLogger(YoutubeMixProvider.class);

  private static final int MIX_QUEUE_CAPACITY = 5000;

  private final ThreadPoolExecutor mixLoadingExecutor;
  private final YoutubeTrackDetailsLoader trackDetailsLoader;

  public DefaultYoutubeMixProvider(YoutubeTrackDetailsLoader trackDetailsLoader) {
    this.trackDetailsLoader = trackDetailsLoader;
    mixLoadingExecutor = new ThreadPoolExecutor(0, 10, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<>(MIX_QUEUE_CAPACITY),
        new DaemonThreadFactory("yt-mix"));
  }

  /**
   * @param maximumPoolSize Maximum number of threads in mix loader thread pool.
   */
  public void setLoaderMaximumPoolSize(int maximumPoolSize) {
    mixLoadingExecutor.setMaximumPoolSize(maximumPoolSize);
  }

  @Override
  public void close() {
    ExecutorTools.shutdownExecutor(mixLoadingExecutor, "youtube mix");
  }

  /**
   * Loads tracks from mix in parallel into a playlist entry.
   *
   * @param mixId ID of the mix
   * @param selectedVideoId Selected track, {@link AudioPlaylist#getSelectedTrack()} will return this.
   * @return Playlist of the tracks in the mix.
   */
  @Override
  public AudioInfoEntity loadMixWithId(
      HttpInterfaceManager httpInterfaceManager,
      String mixId,
      String selectedVideoId,
      AudioTrackInfoTemplate template
  ) {
    List<String> videoIds = new ArrayList<>();

    try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
      String mixUrl = "https://www.youtube.com/watch?v=" + selectedVideoId + "&list=" + mixId;

      try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(mixUrl))) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (!HttpClientTools.isSuccessWithContent(statusCode)) {
          throw new IOException("Invalid status code for mix response: " + statusCode);
        }

        Document document = Jsoup.parse(response.getEntity().getContent(), StandardCharsets.UTF_8.name(), "");
        extractVideoIdsFromMix(document, videoIds);

        if (videoIds.isEmpty() && !document.select("#player-unavailable").isEmpty()) {
          return AudioInfoEntity.NO_INFO;
        }
      }
    } catch (IOException e) {
      throw new FriendlyException("Could not read mix page.", SUSPICIOUS, e);
    }

    if (videoIds.isEmpty()) {
      throw new FriendlyException("Could not find tracks from mix.", SUSPICIOUS, null);
    }

    return loadTracksAsynchronously(httpInterfaceManager, videoIds, selectedVideoId, template);
  }

  private void extractVideoIdsFromMix(Document document, List<String> videoIds) {
    for (Element videoList : document.select("#playlist-autoscroll-list")) {
      for (Element item : videoList.select("li")) {
        videoIds.add(item.attr("data-video-id"));
      }
    }
  }

  private AudioPlaylist loadTracksAsynchronously(
      HttpInterfaceManager httpInterfaceManager,
      List<String> videoIds,
      String selectedVideoId,
      AudioTrackInfoTemplate template
  ) {
    ExecutorCompletionService<AudioInfoEntity> completion = new ExecutorCompletionService<>(mixLoadingExecutor);
    List<AudioTrackInfo> tracks = new ArrayList<>();

    for (final String videoId : videoIds) {
      completion.submit(() -> {
        try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
          return trackDetailsLoader.loadDetails(httpInterface, videoId, template).getTrackInfo();
        }
      });
    }

    try {
      fetchTrackResultsFromExecutor(completion, tracks, videoIds.size());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    AudioTrackInfo selectedTrack = findSelectedTrack(tracks, selectedVideoId);

    if (tracks.isEmpty()) {
      throw new FriendlyException("No tracks from the mix loaded succesfully.", SUSPICIOUS, null);
    } else if (selectedTrack == null) {
      throw new FriendlyException("The selected track of the mix failed to load.", SUSPICIOUS, null);
    }

    return new BasicAudioPlaylist("YouTube mix", tracks, selectedTrack, false);
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

  private void fetchTrackResultsFromExecutor(
      ExecutorCompletionService<AudioInfoEntity> completion,
      List<AudioTrackInfo> tracks,
      int size
  ) throws InterruptedException {
    for (int i = 0; i < size; i++) {
      try {
        AudioInfoEntity item = completion.take().get();

        if (item instanceof AudioTrackInfo) {
          tracks.add((AudioTrackInfo) item);
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
