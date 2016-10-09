package com.sedmelluq.discord.lavaplayer.player;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.OrderedExecutor;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioTrackExecutor;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.FAULT;

/**
 * Audio player manager which is used for creating audio players and loading tracks and playlists.
 */
public class AudioPlayerManager {
  private static final Logger log = LoggerFactory.getLogger(AudioPlayerManager.class);

  private final List<AudioSourceManager> sourceManagers;
  private final ExecutorService trackPlaybackExecutorService;
  private final ExecutorService trackInfoExecutorService;
  private final OrderedExecutor orderedInfoExecutor;
  private volatile long trackStuckThreshold;
  private volatile AudioConfiguration configuration;

  /**
   * Create a new instance
   */
  public AudioPlayerManager() {
    sourceManagers = new ArrayList<>();
    trackPlaybackExecutorService = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 10, TimeUnit.SECONDS, new SynchronousQueue<>());
    trackInfoExecutorService = new ThreadPoolExecutor(1, 5, 30, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
    trackStuckThreshold = TimeUnit.MILLISECONDS.toNanos(10000);
    configuration = new AudioConfiguration();
    orderedInfoExecutor = new OrderedExecutor(trackInfoExecutorService);
  }

  /**
   * @param sourceManager The source manager to register, which will be used for subsequent loadItem calls
   */
  public void registerSourceManager(AudioSourceManager sourceManager) {
    sourceManagers.add(sourceManager);
  }

  /**
   * Schedules loading a track or playlist with the specified identifier.
   * @param identifier    The identifier that a specific source manager should be able to find the track with.
   * @param resultHandler A handler to process the result of this operation. It can either end by finding a track,
   *                      finding a playlist, finding nothing or terminating with an exception.
   * @return A future for this operation
   */
  public Future<Void> loadItem(final String identifier, final AudioLoadResultHandler resultHandler) {
    return trackInfoExecutorService.submit(createItemLoader(identifier, resultHandler));
  }

  /**
   * Schedules loading a track or playlist with the specified identifier with an ordering key so that items with the
   * same ordering key are handled sequentially in the order of calls to this method.
   *
   * @param orderingKey   Object to use as the key for the ordering channel
   * @param identifier    The identifier that a specific source manager should be able to find the track with.
   * @param resultHandler A handler to process the result of this operation. It can either end by finding a track,
   *                      finding a playlist, finding nothing or terminating with an exception.
   * @return A future for this operation
   */
  public Future<Void> loadItemOrdered(Object orderingKey, final String identifier, final AudioLoadResultHandler resultHandler) {
    return orderedInfoExecutor.submit(orderingKey, createItemLoader(identifier, resultHandler));
  }

  private Callable<Void> createItemLoader(final String identifier, final AudioLoadResultHandler resultHandler) {
    return () -> {
      try {
        if (!checkSourcesForItem(identifier, resultHandler)) {
          log.debug("No matches for track with identifier {}.", identifier);
          resultHandler.noMatches();
        }
      } catch (Throwable throwable) {
        FriendlyException exception = ExceptionTools.wrapUnfriendlyExceptions("Something went wrong when looking up the track", FAULT, throwable);
        ExceptionTools.log(log, exception, "loading item " + identifier);

        resultHandler.loadFailed(exception);

        ExceptionTools.rethrowErrors(throwable);
      }

      return null;
    };
  }

  /**
   * Executes an audio track with the given player and volume.
   * @param player The associated player where track events go to
   * @param track The audio track to execute
   * @param volumeLevel The mutable volume level to use
   */
  public void executeTrack(final AudioPlayer player, InternalAudioTrack track, AtomicInteger volumeLevel) {
    final AudioTrackExecutor executor = createExecutorForTrack(track, volumeLevel);
    track.assignExecutor(executor);

    trackPlaybackExecutorService.execute(() -> executor.execute(player));
  }

  private AudioTrackExecutor createExecutorForTrack(InternalAudioTrack track, AtomicInteger volumeLevel) {
    return new LocalAudioTrackExecutor(track, configuration, volumeLevel);
  }

  public void setTrackStuckThreshold(long trackStuckThreshold) {
    this.trackStuckThreshold = TimeUnit.MILLISECONDS.toNanos(trackStuckThreshold);
  }

  public AudioConfiguration getConfiguration() {
    return configuration;
  }

  public long getTrackStuckThresholdNanos() {
    return trackStuckThreshold;
  }

  private boolean checkSourcesForItem(String identifier, AudioLoadResultHandler resultHandler) {
    for (AudioSourceManager sourceManager : sourceManagers) {
      AudioItem item = sourceManager.loadItem(this, identifier);

      if (item != null) {
        if (item instanceof AudioTrack) {
          log.debug("Loaded a track with identifier {} using {}.", identifier, sourceManager.getClass().getSimpleName());
          resultHandler.trackLoaded((AudioTrack) item);
        } else if (item instanceof AudioPlaylist) {
          log.debug("Loaded a playlist with identifier {} using {}.", identifier, sourceManager.getClass().getSimpleName());
          resultHandler.playlistLoaded((AudioPlaylist) item);
        }
        return true;
      }
    }

    return false;
  }

  /**
   * Creates an instance of audio player.
   *
   * @return The new audio player instance.
   */
  public AudioPlayer createPlayer() {
    return new AudioPlayer(this);
  }
}
