package lavaplayer.manager

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.cancel
import lavaplayer.common.tools.DaemonThreadFactory
import lavaplayer.common.tools.ExecutorTools
import lavaplayer.source.ItemSourceManager
import lavaplayer.tools.GarbageCollectionMonitor
import lavaplayer.tools.extensions.addListener
import lavaplayer.tools.io.BuilderConfigurator
import lavaplayer.tools.io.HttpConfigurable
import lavaplayer.tools.io.RequestConfigurator
import lavaplayer.track.DefaultTrackEncoder
import lavaplayer.track.InternalAudioTrack
import lavaplayer.track.TrackStateListener
import lavaplayer.track.loader.DefaultItemLoaderFactory
import lavaplayer.track.loader.ItemLoaderFactory
import lavaplayer.track.playback.AudioTrackExecutor
import lavaplayer.track.playback.LocalAudioTrackExecutor
import java.util.*
import java.util.concurrent.*
import java.util.function.Consumer

/**
 * The default implementation of audio player manager.
 */
open class DefaultAudioPlayerManager : DefaultTrackEncoder(), AudioPlayerManager {
    companion object {
        private val DEFAULT_FRAME_BUFFER_DURATION = TimeUnit.SECONDS.toMillis(5)
        private val DEFAULT_CLEANUP_THRESHOLD = TimeUnit.MINUTES.toMillis(1)
    }

    // Executors
    private val trackPlaybackExecutorService: ExecutorService =
        ThreadPoolExecutor(1, Int.MAX_VALUE, 10, TimeUnit.SECONDS, SynchronousQueue(), DaemonThreadFactory("playback"))
    private val scheduledExecutorService = Executors.newScheduledThreadPool(1, DaemonThreadFactory("manager"))
    private val cleanupThreshold = atomic(DEFAULT_CLEANUP_THRESHOLD)

    // Additional services
    private val garbageCollectionMonitor = GarbageCollectionMonitor(scheduledExecutorService)
    private val lifecycleManager = AudioPlayerLifecycleManager(scheduledExecutorService, cleanupThreshold)

    @Volatile
    private var httpConfigurator: RequestConfigurator? = null

    @Volatile
    private var httpBuilderConfigurator: BuilderConfigurator? = null

    // Configuration
    @Volatile
    override var isUsingSeekGhosting: Boolean = true
    override val configuration = AudioConfiguration()
    override val items: ItemLoaderFactory = DefaultItemLoaderFactory(this)

    @Volatile
    var trackStuckThresholdNanos: Long = TimeUnit.MILLISECONDS.toNanos(10000)

    @Volatile
    override var frameBufferDuration: Int = DEFAULT_FRAME_BUFFER_DURATION.toInt()
        set(value) {
            field = 200.coerceAtLeast(value)
        }

    override val sourceManagers: List<ItemSourceManager>
        get() = _sources

    private val _sources = mutableListOf<ItemSourceManager>()

    override fun shutdown() {
        items.shutdown()
        garbageCollectionMonitor.disable()
        lifecycleManager.shutdown()
        sourceManagers.forEach(Consumer { obj: ItemSourceManager -> obj.shutdown() })
        ExecutorTools.shutdownExecutor(trackPlaybackExecutorService, "track playback")
        ExecutorTools.shutdownExecutor(scheduledExecutorService, "scheduled operations")
        cancel()
    }

    override fun enableGcMonitoring() {
        garbageCollectionMonitor.enable()
    }

    override fun registerSourceManager(sourceManager: ItemSourceManager) {
        _sources.add(sourceManager)
        if (sourceManager is HttpConfigurable) {
            val configurator = httpConfigurator
            if (configurator != null) {
                sourceManager.configureRequests(configurator)
            }

            val builderConfigurator = httpBuilderConfigurator
            if (builderConfigurator != null) {
                sourceManager.configureBuilder(builderConfigurator)
            }
        }
    }

    override fun <T : ItemSourceManager> source(klass: Class<T>): T? {
        for (sourceManager in sourceManagers) {
            if (klass.isAssignableFrom(sourceManager::class.java)) {
                return sourceManager as? T
            }
        }

        return null
    }

    /**
     * Executes an audio track with the given player and volume.
     *
     * @param listener      A listener for track state events
     * @param track         The audio track to execute
     * @param configuration The audio configuration to use for executing
     * @param playerOptions Options of the audio player
     */
    fun executeTrack(
        listener: TrackStateListener?,
        track: InternalAudioTrack,
        configuration: AudioConfiguration,
        playerOptions: AudioPlayerResources
    ) {
        val executor = createExecutorForTrack(track, configuration, playerOptions)
        track.assignExecutor(executor, true)
        trackPlaybackExecutorService.execute { executor.execute(listener) }
    }

    private fun createExecutorForTrack(
        track: InternalAudioTrack,
        configuration: AudioConfiguration,
        playerOptions: AudioPlayerResources
    ): AudioTrackExecutor {
        val customExecutor = track.createLocalExecutor(this)
        return if (customExecutor != null) {
            customExecutor
        } else {
            val bufferDuration = Optional.ofNullable(playerOptions.frameBufferDuration).orElse(frameBufferDuration)
            LocalAudioTrackExecutor(track, configuration, playerOptions, isUsingSeekGhosting, bufferDuration)
        }
    }

    override fun setUseSeekGhosting(useSeekGhosting: Boolean) {
        isUsingSeekGhosting = useSeekGhosting
    }

    override fun setTrackStuckThreshold(trackStuckThreshold: Long) {
        trackStuckThresholdNanos = TimeUnit.MILLISECONDS.toNanos(trackStuckThreshold)
    }

    override fun setPlayerCleanupThreshold(cleanupThreshold: Long) {
        this.cleanupThreshold.value = cleanupThreshold
    }

    override fun createPlayer(): AudioPlayer {
        val player = constructPlayer()
        player.addListener(lifecycleManager)
        return player
    }

    override fun setHttpRequestConfigurator(configurator: RequestConfigurator?) {
        httpConfigurator = configurator
        if (configurator != null) {
            sourceManagers
                .filterIsInstance<HttpConfigurable>()
                .forEach { it.configureRequests(configurator) }
        }
    }

    override fun setHttpBuilderConfigurator(configurator: BuilderConfigurator?) {
        httpBuilderConfigurator = configurator
        if (configurator != null) {
            sourceManagers
                .filterIsInstance<HttpConfigurable>()
                .forEach { it.configureBuilder(configurator) }
        }
    }

    private fun constructPlayer(): AudioPlayer {
        return DefaultAudioPlayer(this)
    }
}
