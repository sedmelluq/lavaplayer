package lavaplayer.manager;

import lavaplayer.common.tools.DaemonThreadFactory;
import lavaplayer.common.tools.ExecutorTools;
import lavaplayer.source.ItemSourceManager;
import lavaplayer.tools.GarbageCollectionMonitor;
import lavaplayer.tools.io.HttpConfigurable;
import lavaplayer.track.*;
import lavaplayer.track.loading.DefaultItemLoaderFactory;
import lavaplayer.track.loading.ItemLoaderFactory;
import lavaplayer.track.playback.AudioTrackExecutor;
import lavaplayer.track.playback.LocalAudioTrackExecutor;
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

/**
 * The default implementation of audio player manager.
 */
public class DefaultAudioPlayerManager extends DefaultTrackEncoder implements AudioPlayerManager {
    private static final int DEFAULT_FRAME_BUFFER_DURATION = (int) TimeUnit.SECONDS.toMillis(5);
    private static final int DEFAULT_CLEANUP_THRESHOLD = (int) TimeUnit.MINUTES.toMillis(1);

    private static final Logger log = LoggerFactory.getLogger(DefaultAudioPlayerManager.class);

    private final List<ItemSourceManager> sourceManagers;

    // Executors
    private final ExecutorService trackPlaybackExecutorService;
    private final ScheduledExecutorService scheduledExecutorService;
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

    public final ItemLoaderFactory itemLoaders;

    /**
     * Create a new instance
     */
    public DefaultAudioPlayerManager() {
        sourceManagers = new ArrayList<>();

        // Executors
        trackPlaybackExecutorService = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 10, TimeUnit.SECONDS, new SynchronousQueue<>(), new DaemonThreadFactory("playback"));
        scheduledExecutorService = Executors.newScheduledThreadPool(1, new DaemonThreadFactory("manager"));

        // Configuration
        trackStuckThreshold = TimeUnit.MILLISECONDS.toNanos(10000);
        configuration = new AudioConfiguration();
        cleanupThreshold = new AtomicLong(DEFAULT_CLEANUP_THRESHOLD);
        frameBufferDuration = DEFAULT_FRAME_BUFFER_DURATION;
        useSeekGhosting = true;

        // Additional services
        itemLoaders = new DefaultItemLoaderFactory(this);
        garbageCollectionMonitor = new GarbageCollectionMonitor(scheduledExecutorService);
        lifecycleManager = new AudioPlayerLifecycleManager(scheduledExecutorService, cleanupThreshold);
        lifecycleManager.initialise();
    }

    @Override
    public void shutdown() {
        garbageCollectionMonitor.disable();
        lifecycleManager.shutdown();

        for (ItemSourceManager sourceManager : sourceManagers) {
            sourceManager.shutdown();
        }

        ExecutorTools.shutdownExecutor(trackPlaybackExecutorService, "track playback");
        ExecutorTools.shutdownExecutor(scheduledExecutorService, "scheduled operations");
    }

    @NotNull
    @Override
    public List<ItemSourceManager> getSourceManagers() {
        return sourceManagers;
    }

    @Override
    public void enableGcMonitoring() {
        garbageCollectionMonitor.enable();
    }

    @Override
    public void registerSourceManager(@NotNull ItemSourceManager sourceManager) {
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
    public <T extends ItemSourceManager> T source(Class<T> klass) {
        for (ItemSourceManager sourceManager : sourceManagers) {
            if (klass.isAssignableFrom(sourceManager.getClass())) {
                return (T) sourceManager;
            }
        }

        return null;
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
                             AudioPlayerResources playerOptions) {

        final AudioTrackExecutor executor = createExecutorForTrack(track, configuration, playerOptions);
        track.assignExecutor(executor, true);

        trackPlaybackExecutorService.execute(() -> executor.execute(listener));
    }

    private AudioTrackExecutor createExecutorForTrack(InternalAudioTrack track, AudioConfiguration configuration,
                                                      AudioPlayerResources playerOptions) {

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
    public ItemLoaderFactory getItemLoaders() {
        return itemLoaders;
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
            for (ItemSourceManager sourceManager : sourceManagers) {
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
            for (ItemSourceManager sourceManager : sourceManagers) {
                if (sourceManager instanceof HttpConfigurable) {
                    ((HttpConfigurable) sourceManager).configureBuilder(configurator);
                }
            }
        }
    }
}
