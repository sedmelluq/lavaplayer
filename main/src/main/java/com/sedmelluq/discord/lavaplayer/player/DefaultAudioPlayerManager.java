package com.sedmelluq.discord.lavaplayer.player;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.ProbingAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.GarbageCollectionMonitor;
import com.sedmelluq.discord.lavaplayer.tools.OrderedExecutor;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackCollection;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioTrackExecutor;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import com.sedmelluq.lava.common.tools.DaemonThreadFactory;
import com.sedmelluq.lava.common.tools.ExecutorTools;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.FAULT;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * The default implementation of audio player manager.
 */
public class DefaultAudioPlayerManager extends DefaultTrackEncoder implements AudioPlayerManager {
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
    private final AtomicLong cleanupThreshold;
    // Additional services
    private final GarbageCollectionMonitor garbageCollectionMonitor;
    private final AudioPlayerLifecycleManager lifecycleManager;
    private volatile Function<RequestConfig, RequestConfig> httpConfigurator;
    private volatile Consumer<HttpClientBuilder> httpBuilderConfigurator;
    // Configuration
    private volatile long trackStuckThreshold;
    private volatile AudioConfiguration configuration;
    private volatile int frameBufferDuration;
    private volatile boolean useSeekGhosting;


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

        // Additional services
        garbageCollectionMonitor = new GarbageCollectionMonitor(scheduledExecutorService);
        lifecycleManager = new AudioPlayerLifecycleManager(scheduledExecutorService, cleanupThreshold);
        lifecycleManager.initialise();
    }

    @Override
    public void shutdown() {
        garbageCollectionMonitor.disable();
        lifecycleManager.shutdown();

        for (AudioSourceManager sourceManager : sourceManagers) {
            sourceManager.shutdown();
        }

        ExecutorTools.shutdownExecutor(trackPlaybackExecutorService, "track playback");
        ExecutorTools.shutdownExecutor(trackInfoExecutorService, "track info");
        ExecutorTools.shutdownExecutor(scheduledExecutorService, "scheduled operations");
    }

    @NotNull
    @Override
    public List<AudioSourceManager> getSourceManagers() {
        return sourceManagers;
    }

    @Override
    public void enableGcMonitoring() {
        garbageCollectionMonitor.enable();
    }

    @Override
    public void registerSourceManager(AudioSourceManager sourceManager) {
        sourceManagers.add(sourceManager);

        if (sourceManager instanceof HttpConfigurable) {
            Function<RequestConfig, RequestConfig> configurator = httpConfigurator;

            if (configurator != null) {
                ((HttpConfigurable) sourceManager).configureRequests(configurator);
            }

            Consumer<HttpClientBuilder> builderConfigurator = httpBuilderConfigurator;

            if (builderConfigurator != null) {
                ((HttpConfigurable) sourceManager).configureBuilder(builderConfigurator);
            }
        }
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
    public Future<Void> loadItem(final AudioReference reference, final AudioLoadResultHandler resultHandler) {
        try {
            return trackInfoExecutorService.submit(createItemLoader(reference, resultHandler));
        } catch (RejectedExecutionException e) {
            return handleLoadRejected(reference.identifier, resultHandler, e);
        }
    }

    @Override
    public Future<Void> loadItemOrdered(Object orderingKey, final AudioReference reference, final AudioLoadResultHandler resultHandler) {
        try {
            return orderedInfoExecutor.submit(orderingKey, createItemLoader(reference, resultHandler));
        } catch (RejectedExecutionException e) {
            return handleLoadRejected(reference.identifier, resultHandler, e);
        }
    }

    private Future<Void> handleLoadRejected(String identifier, AudioLoadResultHandler resultHandler, RejectedExecutionException e) {
        FriendlyException exception = new FriendlyException("Cannot queue loading a track, queue is full.", SUSPICIOUS, e);
        ExceptionTools.log(log, exception, "queueing item " + identifier);

        resultHandler.loadFailed(exception);

        return ExecutorTools.COMPLETED_VOID;
    }

    private Callable<Void> createItemLoader(final AudioReference reference, final AudioLoadResultHandler resultHandler) {
        return () -> {
            boolean[] reported = new boolean[1];

            try {
                if (!checkSourcesForItem(reference, resultHandler, reported)) {
                    log.debug("No matches for track with identifier {}.", reference.identifier);
                    resultHandler.noMatches();
                }
            } catch (Throwable throwable) {
                if (reported[0]) {
                    log.warn("Load result handler for {} threw an exception", reference.identifier, throwable);
                } else {
                    dispatchItemLoadFailure(reference.identifier, resultHandler, throwable);
                }

                ExceptionTools.rethrowErrors(throwable);
            }

            return null;
        };
    }

    private void dispatchItemLoadFailure(String identifier, AudioLoadResultHandler resultHandler, Throwable throwable) {
        FriendlyException exception = ExceptionTools.wrapUnfriendlyExceptions("Something went wrong when looking up the track", FAULT, throwable);
        ExceptionTools.log(log, exception, "loading item " + identifier);

        resultHandler.loadFailed(exception);
    }

    /**
     * Executes an audio track with the given player and volume.
     *
     * @param listener      A listener for track state events
     * @param track         The audio track to execute
     * @param configuration The audio configuration to use for executing
     * @param playerOptions Options of the audio player
     */
    public void executeTrack(TrackStateListener listener, InternalAudioTrack track, AudioConfiguration configuration,
                             AudioPlayerOptions playerOptions) {

        final AudioTrackExecutor executor = createExecutorForTrack(track, configuration, playerOptions);
        track.assignExecutor(executor, true);

        trackPlaybackExecutorService.execute(() -> executor.execute(listener));
    }

    private AudioTrackExecutor createExecutorForTrack(InternalAudioTrack track, AudioConfiguration configuration,
                                                      AudioPlayerOptions playerOptions) {

        AudioTrackExecutor customExecutor = track.createLocalExecutor(this);

        if (customExecutor != null) {
            return customExecutor;
        } else {
            int bufferDuration = Optional.ofNullable(playerOptions.frameBufferDuration.get()).orElse(frameBufferDuration);
            return new LocalAudioTrackExecutor(track, configuration, playerOptions, useSeekGhosting, bufferDuration);
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

    private boolean checkSourcesForItem(AudioReference reference, AudioLoadResultHandler resultHandler, boolean[] reported) {
        AudioReference currentReference = reference;

        for (int redirects = 0; redirects < MAXIMUM_LOAD_REDIRECTS && currentReference.identifier != null; redirects++) {
            AudioItem item = checkSourcesForItemOnce(currentReference, resultHandler, reported);
            if (item == null) {
                return false;
            } else if (!(item instanceof AudioReference)) {
                return true;
            }
            currentReference = (AudioReference) item;
        }

        return false;
    }

    private AudioItem checkSourcesForItemOnce(AudioReference reference, AudioLoadResultHandler resultHandler, boolean[] reported) {
        for (AudioSourceManager sourceManager : sourceManagers) {
            if (reference.containerDescriptor != null && !(sourceManager instanceof ProbingAudioSourceManager)) {
                continue;
            }

            AudioItem item = sourceManager.loadItem(this, reference);

            if (item != null) {
                if (item instanceof AudioTrack) {
                    log.debug("Loaded a track with identifier {} using {}.", reference.identifier, sourceManager.getClass().getSimpleName());
                    reported[0] = true;
                    resultHandler.trackLoaded((AudioTrack) item);
                } else if (item instanceof AudioTrackCollection) {
                    log.debug("Loaded an audio track collection with identifier {} using {}.", reference.identifier, sourceManager.getClass().getSimpleName());
                    reported[0] = true;
                    resultHandler.collectionLoaded((AudioTrackCollection) item);
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
        AudioPlayer player = constructPlayer();
        player.addListener(lifecycleManager);

        return player;
    }

    protected AudioPlayer constructPlayer() {
        return new DefaultAudioPlayer(this);
    }

    @Override
    public void setHttpRequestConfigurator(Function<RequestConfig, RequestConfig> configurator) {
        this.httpConfigurator = configurator;

        if (configurator != null) {
            for (AudioSourceManager sourceManager : sourceManagers) {
                if (sourceManager instanceof HttpConfigurable) {
                    ((HttpConfigurable) sourceManager).configureRequests(configurator);
                }
            }
        }
    }

    @Override
    public void setHttpBuilderConfigurator(Consumer<HttpClientBuilder> configurator) {
        this.httpBuilderConfigurator = configurator;

        if (configurator != null) {
            for (AudioSourceManager sourceManager : sourceManagers) {
                if (sourceManager instanceof HttpConfigurable) {
                    ((HttpConfigurable) sourceManager).configureBuilder(configurator);
                }
            }
        }
    }
}
