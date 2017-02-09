package com.sedmelluq.discord.lavaplayer.player;

import com.sedmelluq.discord.lavaplayer.player.hook.AudioOutputHook;
import com.sedmelluq.discord.lavaplayer.player.hook.AudioOutputHookFactory;
import com.sedmelluq.discord.lavaplayer.remote.RemoteAudioTrackExecutor;
import com.sedmelluq.discord.lavaplayer.remote.RemoteNodeManager;
import com.sedmelluq.discord.lavaplayer.remote.RemoteNodeRegistry;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.DaemonThreadFactory;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.ExecutorTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.OrderedExecutor;
import com.sedmelluq.discord.lavaplayer.tools.GarbageCollectionMonitor;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DecodedTrackHolder;
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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.FAULT;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * The default implementation of audio player manager.
 */
public class DefaultAudioPlayerManager implements AudioPlayerManager {
  private static final int DEFAULT_FRAME_BUFFER_DURATION = (int) TimeUnit.SECONDS.toMillis(5);
  private static final int DEFAULT_CLEANUP_THRESHOLD = (int) TimeUnit.MINUTES.toMillis(1);

  private static final int MAXIMUM_LOAD_REDIRECTS = 5;
  private static final int DEFAULT_LOADER_POOL_SIZE = 10;
  private static final int LOADER_QUEUE_CAPACITY = 5000;

  private static final Logger log = LoggerFactory.getLogger(DefaultAudioPlayerManager.class);

  private final List<AudioSourceManager> sourceManagers;

  // Executors
  private final ExecutorService trackPlaybackExecutorService;
  private final ThreadPoolExecutor trackInfoExecutorService;
  private final ScheduledExecutorService scheduledExecutorService;
  private final OrderedExecutor orderedInfoExecutor;

  // Configuration
  private volatile long trackStuckThreshold;
  private volatile AudioConfiguration configuration;
  private final AtomicLong cleanupThreshold;
  private volatile int frameBufferDuration;
  private volatile boolean useSeekGhosting;
  private volatile AudioOutputHookFactory outputHookFactory;

  // Additional services
  private final RemoteNodeManager remoteNodeManager;
  private final GarbageCollectionMonitor garbageCollectionMonitor;
  private final AudioPlayerLifecycleManager lifecycleManager;


  /**
   * Create a new instance
   */
  public DefaultAudioPlayerManager() {
    sourceManagers = new ArrayList<>();

    // Executors
    trackPlaybackExecutorService = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 10, TimeUnit.SECONDS,
        new SynchronousQueue<>(), new DaemonThreadFactory("playback"));
    trackInfoExecutorService = ExecutorTools.createEagerlyScalingExecutor(1, DEFAULT_LOADER_POOL_SIZE,
        TimeUnit.SECONDS.toMillis(30), LOADER_QUEUE_CAPACITY, new DaemonThreadFactory("info-loader"));
    scheduledExecutorService = Executors.newScheduledThreadPool(1, new DaemonThreadFactory("manager"));
    orderedInfoExecutor = new OrderedExecutor(trackInfoExecutorService);

    // Configuration
    trackStuckThreshold = TimeUnit.MILLISECONDS.toNanos(10000);
    configuration = new AudioConfiguration();
    cleanupThreshold = new AtomicLong(DEFAULT_CLEANUP_THRESHOLD);
    frameBufferDuration = DEFAULT_FRAME_BUFFER_DURATION;
    useSeekGhosting = true;
    outputHookFactory = null;

    // Additional services
    remoteNodeManager = new RemoteNodeManager(this);
    garbageCollectionMonitor = new GarbageCollectionMonitor(scheduledExecutorService);
    lifecycleManager = new AudioPlayerLifecycleManager(scheduledExecutorService, cleanupThreshold);
    lifecycleManager.initialise();
  }

  @Override
  public void shutdown() {
    remoteNodeManager.shutdown();
    garbageCollectionMonitor.disable();
    lifecycleManager.shutdown();

    for (AudioSourceManager sourceManager : sourceManagers) {
      sourceManager.shutdown();
    }

    ExecutorTools.shutdownExecutor(trackPlaybackExecutorService, "track playback");
    ExecutorTools.shutdownExecutor(trackInfoExecutorService, "track info");
    ExecutorTools.shutdownExecutor(scheduledExecutorService, "scheduled operations");
  }

  @Override
  public void setOutputHookFactory(AudioOutputHookFactory outputHookFactory) {
    this.outputHookFactory = outputHookFactory;
  }

  @Override
  public void useRemoteNodes(String... nodeAddresses) {
    if (nodeAddresses.length > 0) {
      remoteNodeManager.initialise(Arrays.asList(nodeAddresses));
    } else {
      remoteNodeManager.shutdown();
    }
  }

  @Override
  public void enableGcMonitoring() {
    garbageCollectionMonitor.enable();
  }

  @Override
  public void registerSourceManager(AudioSourceManager sourceManager) {
    sourceManagers.add(sourceManager);
  }

  @Override
  public <T extends AudioSourceManager> T source(Class<T> klass) {
    for (AudioSourceManager sourceManager : sourceManagers) {
      if (klass.isAssignableFrom(sourceManager.getClass())) {
        return (T) sourceManager;
      }
    }

    return null;
  }

  @Override
  public Future<Void> loadItem(final String identifier, final AudioLoadResultHandler resultHandler) {
    try {
      return trackInfoExecutorService.submit(createItemLoader(identifier, resultHandler));
    } catch (RejectedExecutionException e) {
      return handleLoadRejected(identifier, resultHandler, e);
    }
  }

  @Override
  public Future<Void> loadItemOrdered(Object orderingKey, final String identifier, final AudioLoadResultHandler resultHandler) {
    try {
      return orderedInfoExecutor.submit(orderingKey, createItemLoader(identifier, resultHandler));
    } catch (RejectedExecutionException e) {
      return handleLoadRejected(identifier, resultHandler, e);
    }
  }

  private Future<Void> handleLoadRejected(String identifier, AudioLoadResultHandler resultHandler, RejectedExecutionException e) {
    FriendlyException exception = new FriendlyException("Cannot queue loading a track, queue is full.", SUSPICIOUS, e);
    ExceptionTools.log(log, exception, "queueing item " + identifier);

    resultHandler.loadFailed(exception);

    return ExecutorTools.COMPLETED_VOID;
  }

  private Callable<Void> createItemLoader(final String identifier, final AudioLoadResultHandler resultHandler) {
    return () -> {
      try {
        if (!checkSourcesForItem(new AudioReference(identifier, null), resultHandler)) {
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

  @Override
  public void encodeTrack(MessageOutput stream, AudioTrack track) throws IOException {
    DataOutput output = stream.startMessage();

    AudioTrackInfo trackInfo = track.getInfo();
    output.writeUTF(trackInfo.title);
    output.writeUTF(trackInfo.author);
    output.writeLong(trackInfo.length);
    output.writeUTF(trackInfo.identifier);
    output.writeBoolean(trackInfo.isStream);

    encodeTrackDetails(track, output);
    output.writeLong(track.getPosition());

    stream.commitMessage();
  }

  @Override
  public DecodedTrackHolder decodeTrack(MessageInput stream) throws IOException {
    DataInput input = stream.nextMessage();
    if (input == null) {
      return null;
    }

    AudioTrackInfo trackInfo = new AudioTrackInfo(input.readUTF(), input.readUTF(), input.readLong(), input.readUTF(), input.readBoolean());
    AudioTrack track = decodeTrackDetails(trackInfo, input);
    long position = input.readLong();

    if (track != null) {
      track.setPosition(position);
    }

    stream.skipRemainingBytes();

    return new DecodedTrackHolder(track);
  }

  /**
   * Encodes an audio track to a byte array. Does not include AudioTrackInfo in the buffer.
   * @param track The track to encode
   * @return The bytes of the encoded data
   */
  public byte[] encodeTrackDetails(AudioTrack track) {
    try {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      DataOutput output = new DataOutputStream(byteOutput);

      encodeTrackDetails(track, output);

      return byteOutput.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void encodeTrackDetails(AudioTrack track, DataOutput output) throws IOException {
    AudioSourceManager sourceManager = track.getSourceManager();
    output.writeUTF(sourceManager.getSourceName());
    sourceManager.encodeTrack(track, output);
  }

  /**
   * Decodes an audio track from a byte array.
   * @param trackInfo Track info for the track to decode
   * @param buffer Byte array containing the encoded track
   * @return Decoded audio track
   */
  public AudioTrack decodeTrackDetails(AudioTrackInfo trackInfo, byte[] buffer) {
    try {
      DataInput input = new DataInputStream(new ByteArrayInputStream(buffer));
      return decodeTrackDetails(trackInfo, input);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private AudioTrack decodeTrackDetails(AudioTrackInfo trackInfo, DataInput input) throws IOException {
    String sourceName = input.readUTF();

    for (AudioSourceManager sourceManager : sourceManagers) {
      if (sourceName.equals(sourceManager.getSourceName())) {
        return sourceManager.decodeTrack(trackInfo, input);
      }
    }

    return null;
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
    track.assignExecutor(executor, true);

    trackPlaybackExecutorService.execute(() -> executor.execute(listener));
  }

  private AudioTrackExecutor createExecutorForTrack(InternalAudioTrack track, AudioConfiguration configuration, AtomicInteger volumeLevel) {
    AudioSourceManager sourceManager = track.getSourceManager();

    if (remoteNodeManager.isEnabled() && sourceManager != null && sourceManager.isTrackEncodable(track)) {
      return new RemoteAudioTrackExecutor(track, configuration, remoteNodeManager, volumeLevel);
    } else {
      AudioTrackExecutor customExecutor = track.createLocalExecutor(this);

      if (customExecutor != null) {
        return customExecutor;
      } else {
        return new LocalAudioTrackExecutor(track, configuration, volumeLevel, useSeekGhosting, frameBufferDuration);
      }
    }
  }

  @Override
  public AudioConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public boolean isUsingSeekGhosting() {
    return useSeekGhosting;
  }

  @Override
  public void setUseSeekGhosting(boolean useSeekGhosting) {
    this.useSeekGhosting = useSeekGhosting;
  }

  @Override
  public int getFrameBufferDuration() {
    return frameBufferDuration;
  }

  @Override
  public void setFrameBufferDuration(int frameBufferDuration) {
    this.frameBufferDuration = Math.max(200, frameBufferDuration);
  }

  @Override
  public void setTrackStuckThreshold(long trackStuckThreshold) {
    this.trackStuckThreshold = TimeUnit.MILLISECONDS.toNanos(trackStuckThreshold);
  }

  public long getTrackStuckThresholdNanos() {
    return trackStuckThreshold;
  }

  @Override
  public void setPlayerCleanupThreshold(long cleanupThreshold) {
    this.cleanupThreshold.set(cleanupThreshold);
  }

  @Override
  public void setItemLoaderThreadPoolSize(int poolSize) {
    trackInfoExecutorService.setMaximumPoolSize(poolSize);
  }

  private boolean checkSourcesForItem(AudioReference reference, AudioLoadResultHandler resultHandler) {
    AudioReference currentReference = reference;

    for (int redirects = 0; redirects < MAXIMUM_LOAD_REDIRECTS && currentReference.identifier != null; redirects++) {
      AudioItem item = checkSourcesForItemOnce(currentReference, resultHandler);
      if (item == null) {
        return false;
      } else if (!(item instanceof AudioReference)) {
        return true;
      }
      currentReference = (AudioReference) item;
    }

    return false;
  }

  private AudioItem checkSourcesForItemOnce(AudioReference reference, AudioLoadResultHandler resultHandler) {
    for (AudioSourceManager sourceManager : sourceManagers) {
      AudioItem item = sourceManager.loadItem(this, reference);

      if (item != null) {
        if (item instanceof AudioTrack) {
          log.debug("Loaded a track with identifier {} using {}.", reference.identifier, sourceManager.getClass().getSimpleName());
          resultHandler.trackLoaded((AudioTrack) item);
        } else if (item instanceof AudioPlaylist) {
          log.debug("Loaded a playlist with identifier {} using {}.", reference.identifier, sourceManager.getClass().getSimpleName());
          resultHandler.playlistLoaded((AudioPlaylist) item);
        }
        return item;
      }
    }

    return null;
  }

  public ExecutorService getExecutor() {
    return trackPlaybackExecutorService;
  }

  @Override
  public AudioPlayer createPlayer() {
    AudioOutputHook outputHook = outputHookFactory != null ? outputHookFactory.createOutputHook() : null;
    AudioPlayer player = new AudioPlayer(this, outputHook);
    player.addListener(lifecycleManager);

    if (remoteNodeManager.isEnabled()) {
      player.addListener(remoteNodeManager);
    }

    return player;
  }

  @Override
  public RemoteNodeRegistry getRemoteNodeRegistry() {
    return remoteNodeManager;
  }
}
