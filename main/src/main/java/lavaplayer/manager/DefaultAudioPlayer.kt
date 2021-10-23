package lavaplayer.manager

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import lavaplayer.filter.PcmFilterFactory
import lavaplayer.manager.event.*
import lavaplayer.tools.ExceptionTools
import lavaplayer.tools.FriendlyException
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackEndReason
import lavaplayer.track.AudioTrackEndReason.*
import lavaplayer.track.InternalAudioTrack
import lavaplayer.track.TrackStateListener
import lavaplayer.track.playback.AudioFrame
import lavaplayer.track.playback.AudioFrameProviderTools
import lavaplayer.track.playback.LocalAudioTrackExecutor
import lavaplayer.track.playback.MutableAudioFrame
import mu.KotlinLogging
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * An audio player that is capable of playing audio tracks and provides audio frames from the currently playing track.
 */
class DefaultAudioPlayer(private val manager: DefaultAudioPlayerManager) : AudioPlayer, TrackStateListener,
    CoroutineScope {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + manager.coroutineContext

    override var volume: Int
        get() = resources.volumeLevel
        set(value) {
            resources.volumeLevel = 1000.coerceAtMost(0.coerceAtLeast(value))
        }

    override var isPaused: Boolean
        get() = paused.value
        set(value) {
            if (paused.compareAndSet(!value, value)) {
                if (value) {
                    dispatchEvent(PlayerPauseEvent(this))
                } else {
                    dispatchEvent(PlayerResumeEvent(this))
                    lastReceiveTime = System.nanoTime()
                }
            }
        }

    override val playingTrack: AudioTrack?
        get() = activeTrack

    override val events: SharedFlow<AudioEvent>
        get() = eventFlow

    private val paused = atomic(false)
    private val trackSwitchLock = Any()
    private val resources = AudioPlayerResources()
    private val eventFlow = MutableSharedFlow<AudioEvent>(extraBufferCapacity = 1)

    @Volatile
    private var activeTrack: InternalAudioTrack? = null

    @Volatile
    private var lastRequestTime: Long = 0

    @Volatile
    private var lastReceiveTime: Long = 0

    @Volatile
    private var stuckEventSent: Boolean = false

    @Volatile
    private var shadowTrack: InternalAudioTrack? = null

    /**
     * @param track The track to start playing
     */
    override fun playTrack(track: AudioTrack?) {
        startTrack(track, false)
    }

    /**
     * @param track       The track to start playing, passing null will stop the current track and return false
     * @param noInterrupt Whether to only start if nothing else is playing
     * @return True if the track was started
     */
    override fun startTrack(track: AudioTrack?, noInterrupt: Boolean): Boolean {
        val newTrack = track as? InternalAudioTrack
        var previousTrack: InternalAudioTrack?

        synchronized(trackSwitchLock) {
            previousTrack = activeTrack
            if (noInterrupt && previousTrack != null) {
                return false
            }

            activeTrack = newTrack
            lastRequestTime = System.currentTimeMillis()
            lastReceiveTime = System.nanoTime()
            stuckEventSent = false
            if (previousTrack != null) {
                previousTrack!!.stop()
                dispatchEvent(TrackEndEvent(this, previousTrack!!, if (newTrack == null) STOPPED else REPLACED))

                shadowTrack = previousTrack
            }
        }

        if (newTrack == null) {
            shadowTrack = null
            return false
        }

        dispatchEvent(TrackStartEvent(this, newTrack))

        manager.executeTrack(this, newTrack, manager.configuration, resources)
        return true
    }

    /**
     * Stop currently playing track.
     */
    override fun stopTrack() {
        stopWithReason(STOPPED)
    }

    override fun provide(): AudioFrame? {
        return AudioFrameProviderTools.delegateToTimedProvide(this)
    }

    override fun provide(timeout: Long, unit: TimeUnit): AudioFrame? {
        lateinit var track: InternalAudioTrack

        lastRequestTime = System.currentTimeMillis()
        if (timeout == 0L && isPaused) {
            return null
        }

        while (activeTrack?.also { track = it } != null) {
            var frame = if (timeout > 0) track.provide(timeout, unit) else track.provide()
            if (frame != null) {
                lastReceiveTime = System.nanoTime()
                shadowTrack = null

                if (frame.isTerminator) {
                    handleTerminator(track)
                    continue
                }
            } else if (timeout == 0L) {
                checkStuck(track)
                frame = provideShadowFrame()
            }

            return frame
        }

        return null
    }

    override fun provide(targetFrame: MutableAudioFrame): Boolean {
        try {
            return provide(targetFrame, 0, TimeUnit.MILLISECONDS)
        } catch (e: Throwable) {
            ExceptionTools.keepInterrupted(e)
            throw RuntimeException(e)
        }
    }

    override fun provide(targetFrame: MutableAudioFrame, timeout: Long, unit: TimeUnit): Boolean {
        lateinit var track: InternalAudioTrack

        lastRequestTime = System.currentTimeMillis()
        if (timeout == 0L && isPaused) {
            return false
        }

        while (activeTrack?.also { track = it } != null) {
            if (if (timeout > 0) track.provide(targetFrame, timeout, unit) else track.provide(targetFrame)) {
                lastReceiveTime = System.nanoTime()
                shadowTrack = null

                if (targetFrame.isTerminator) {
                    handleTerminator(track)
                    continue
                }

                return true
            } else if (timeout == 0L) {
                checkStuck(track)
                return provideShadowFrame(targetFrame)
            } else {
                return false
            }
        }

        return false
    }

    override fun setFilterFactory(factory: PcmFilterFactory?) {
        resources.filterFactory = factory
    }

    override fun setFrameBufferDuration(duration: Int) {
        resources.frameBufferDuration = duration.let { 200.coerceAtLeast(it) }
    }

    /**
     * Destroy the player and stop playing track.
     */
    override fun destroy() {
        stopTrack()
        cancel()
    }

    override fun onTrackException(track: AudioTrack, exception: FriendlyException) {
        dispatchEvent(TrackExceptionEvent(this, track, exception))
    }

    override fun onTrackStuck(track: AudioTrack, thresholdMs: Long) {
        dispatchEvent(TrackStuckEvent(this, track, thresholdMs))
    }

    /**
     * Check if the player should be "cleaned up" - stopped due to nothing using it, with the given threshold.
     *
     * @param threshold Threshold in milliseconds to use
     */
    override fun checkCleanup(threshold: Long) {
        val track = playingTrack
        if (track != null && System.currentTimeMillis() - lastRequestTime >= threshold) {
            log.debug { "Triggering cleanup on an audio player playing track $track" }
            stopWithReason(CLEANUP)
        }
    }

    private fun stopWithReason(reason: AudioTrackEndReason) {
        shadowTrack = null

        synchronized(trackSwitchLock) {
            val previousTrack = activeTrack
            activeTrack = null
            if (previousTrack != null) {
                previousTrack.stop()
                dispatchEvent(TrackEndEvent(this, previousTrack, reason))
            }
        }
    }

    private fun provideShadowFrame(): AudioFrame? {
        val shadow = shadowTrack
        var frame: AudioFrame? = null

        if (shadow != null) {
            frame = shadow.provide()
            if (frame != null && frame.isTerminator) {
                shadowTrack = null
                frame = null
            }
        }

        return frame
    }

    private fun provideShadowFrame(targetFrame: MutableAudioFrame): Boolean {
        val shadow = shadowTrack
        if (shadow != null && shadow.provide(targetFrame)) {
            if (targetFrame.isTerminator) {
                shadowTrack = null
                return false
            }

            return true
        }

        return false
    }

    private fun dispatchEvent(event: AudioEvent) {
        log.debug { "Firing an event with class ${event::class.qualifiedName}" }
        runBlocking {
            eventFlow.emit(event)
        }
    }

    private fun handleTerminator(track: InternalAudioTrack) {
        synchronized(trackSwitchLock) {
            if (activeTrack == track) {
                activeTrack = null

                dispatchEvent(
                    TrackEndEvent(this, track, if (track.activeExecutor.failedBeforeLoad()) LOAD_FAILED else FINISHED)
                )
            }
        }
    }

    private fun checkStuck(track: AudioTrack) {
        if (!stuckEventSent && System.nanoTime() - lastReceiveTime > manager.trackStuckThresholdNanos) {
            stuckEventSent = true

            val stackTrace = getStackTrace(track)
            val threshold = TimeUnit.NANOSECONDS.toMillis(manager.trackStuckThresholdNanos)

            dispatchEvent(TrackStuckEvent(this, track, threshold, stackTrace ?: emptyList()))
        }
    }

    private fun getStackTrace(track: AudioTrack): List<StackTraceElement>? {
        if (track is InternalAudioTrack) {
            val executor = track.activeExecutor as? LocalAudioTrackExecutor
            return executor?.stackTrace?.toList()
        }

        return null
    }
}
