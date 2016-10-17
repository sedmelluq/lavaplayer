package com.sedmelluq.discord.lavaplayer.player;

import com.sedmelluq.discord.lavaplayer.player.hook.AudioOutputHook;
import com.sedmelluq.discord.lavaplayer.player.hook.AudioOutputHookFactory;
import com.sedmelluq.discord.lavaplayer.remote.RemoteAudioTrackExecutor;
import com.sedmelluq.discord.lavaplayer.remote.RemoteNodeManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.DaemonThreadFactory;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.OrderedExecutor;
import com.sedmelluq.discord.lavaplayer.tools.GarbageCollectionMonitor;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackStateListener;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioTrackExecutor;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
  private static final int DEFAULT_FRAME_BUFFER_DURATION = (int) TimeUnit.SECONDS.toMillis(5);

  private static final Logger log = LoggerFactory.getLogger(AudioPlayerManager.class);

  private final List<AudioSourceManager> sourceManagers;
  private final ExecutorService trackPlaybackExecutorService;
  private final ExecutorService trackInfoExecutorService;
  private final OrderedExecutor orderedInfoExecutor;
  private final RemoteNodeManager remoteNodeManager;
  private final GarbageCollectionMonitor garbageCollectionMonitor;
  private volatile boolean useSeekGhosting;
  private volatile int frameBufferDuration;
  private volatile long trackStuckThreshold;
  private volatile AudioConfiguration configuration;
  private volatile AudioOutputHookFactory outputHookFactory;

  /**
   * Create a new instance
   */
  public AudioPlayerManager() {
    sourceManagers = new ArrayList<>();
    trackPlaybackExecutorService = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 10, TimeUnit.SECONDS,
        new SynchronousQueue<>(), new DaemonThreadFactory("playback"));
    trackInfoExecutorService = new ThreadPoolExecutor(1, 5, 30, TimeUnit.SECONDS, new LinkedBlockingDeque<>(),
        new DaemonThreadFactory("info-loader"));
    trackStuckThreshold = TimeUnit.MILLISECONDS.toNanos(10000);
    configuration = new AudioConfiguration();
    orderedInfoExecutor = new OrderedExecutor(trackInfoExecutorService);
    remoteNodeManager = new RemoteNodeManager(this);
    garbageCollectionMonitor = new GarbageCollectionMonitor();
    useSeekGhosting = true;
    frameBufferDuration = DEFAULT_FRAME_BUFFER_DURATION;
    outputHookFactory = null;
  }

  public void setOutputHookFactory(AudioOutputHookFactory outputHookFactory) {
    this.outputHookFactory = outputHookFactory;
  }

  /**
   * Configure to use remote nodes for playback. Should only be called once.
   * @param nodeAddresses The addresses of the remote nodes
   */
  public void useRemoteNodes(String... nodeAddresses) {
    remoteNodeManager.initialise(Arrays.asList(nodeAddresses));
  }

  /**
   * Enable reporting GC pause length statistics to log (warn level with lengths bad for latency, debug level otherwise)
   */
  public void enableGcMonitoring() {
    garbageCollectionMonitor.enable();
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
   * Encodes an audio track to a byte array. Does not include AudioTrackInfo in the buffer.
   * @param track The track to encode
   * @return The bytes of the encoded data
   */
  public byte[] encodeTrack(AudioTrack track) {
    try {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      DataOutput output = new DataOutputStream(byteOutput);

      AudioSourceManager sourceManager = track.getSourceManager();
      output.writeUTF(sourceManager.getSourceName());
      sourceManager.encodeTrack(track, output);

      return byteOutput.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Decodes an audio track from a byte array.
   * @param trackInfo Track info for the track to decode
   * @param buffer Byte array containing the encoded track
   * @return Decoded audio track
   */
  public AudioTrack decodeTrack(AudioTrackInfo trackInfo, byte[] buffer) {
    try {
      DataInput input = new DataInputStream(new ByteArrayInputStream(buffer));
      String sourceName = input.readUTF();

      for (AudioSourceManager sourceManager : sourceManagers) {
        if (sourceName.equals(sourceManager.getSourceName())) {
          return sourceManager.decodeTrack(trackInfo, input);
        }
      }

      return null;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Executes an audio track with the given player and volume.
   * @param listener A listener for track state events
   * @param track The audio track to execute
   * @param configuration The audio configuration to use for executing
   * @param volumeLevel The mutable volume level to use
   */
  public void executeTrack(final TrackStateListener listener, InternalAudioTrack track, AudioConfiguration configuration, AtomicInteger volumeLevel) {
    final AudioTrackExecutor executor = createExecutorForTrack(track, configuration, volumeLevel);
    track.assignExecutor(executor);

    trackPlaybackExecutorService.execute(() -> executor.execute(listener));
  }

  private AudioTrackExecutor createExecutorForTrack(InternalAudioTrack track, AudioConfiguration configuration, AtomicInteger volumeLevel) {
    AudioSourceManager sourceManager = track.getSourceManager();

    if (remoteNodeManager.isEnabled() && sourceManager != null && sourceManager.isTrackEncodable(track)) {
      return new RemoteAudioTrackExecutor(track, configuration, remoteNodeManager, volumeLevel);
    } else {
      return new LocalAudioTrackExecutor(track, configuration, volumeLevel, useSeekGhosting, frameBufferDuration);
    }
  }

  public AudioConfiguration getConfiguration() {
    return configuration;
  }

  public boolean isUsingSeekGhosting() {
    return useSeekGhosting;
  }

  public void setUseSeekGhosting(boolean useSeekGhosting) {
    this.useSeekGhosting = useSeekGhosting;
  }

  public int getFrameBufferDuration() {
    return frameBufferDuration;
  }

  public void setFrameBufferDuration(int frameBufferDuration) {
    this.frameBufferDuration = Math.max(200, frameBufferDuration);
  }

  public void setTrackStuckThreshold(long trackStuckThreshold) {
    this.trackStuckThreshold = TimeUnit.MILLISECONDS.toNanos(trackStuckThreshold);
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

  public ExecutorService getExecutor() {
    return trackPlaybackExecutorService;
  }

  /**
   * Creates an instance of audio player.
   *
   * @return The new audio player instance.
   */
  public AudioPlayer createPlayer() {
    AudioOutputHook outputHook = outputHookFactory != null ? outputHookFactory.createOutputHook() : null;
    AudioPlayer player = new AudioPlayer(this, outputHook);

    if (remoteNodeManager.isEnabled()) {
      player.addListener(remoteNodeManager);
    }

    return player;
  }
}
