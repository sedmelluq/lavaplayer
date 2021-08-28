package lavaplayer.track

import lavaplayer.manager.AudioPlayerManager
import lavaplayer.source.ItemSourceManager
import lavaplayer.track.playback.AudioFrame
import lavaplayer.track.playback.AudioTrackExecutor
import lavaplayer.track.playback.MutableAudioFrame
import lavaplayer.track.playback.PrimordialAudioTrackExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Abstract base for all audio tracks with an executor
 *
 * @param info Track info
 */
abstract class BaseAudioTrack(final override val info: AudioTrackInfo) : InternalAudioTrack {
    @JvmField
    protected val accurateDuration = AtomicLong()

    private val initialExecutor = PrimordialAudioTrackExecutor(info)
    private val executorAssigned = AtomicBoolean()
    @Volatile
    private var _activeExecutor: AudioTrackExecutor? = null
    @Volatile
    override var userData: Any? = null

    override val activeExecutor: AudioTrackExecutor
        get() = _activeExecutor ?: initialExecutor

    override val sourceManager: ItemSourceManager?
        get() = null

    override val state: AudioTrackState
        get() = activeExecutor.state

    override val identifier: String
        get() = info.identifier

    override val isSeekable: Boolean
        get() = !info.isStream

    override var position: Long
        get() = activeExecutor.position
        set(position) {
            activeExecutor.position = position
        }

    override val duration: Long
        get() {
            val accurate = accurateDuration.get()
            return if (accurate == 0L) info.length else accurate
        }

    override fun assignExecutor(executor: AudioTrackExecutor?, applyPrimordialState: Boolean) {
        _activeExecutor = if (executorAssigned.compareAndSet(false, true)) {
            if (applyPrimordialState) {
                initialExecutor.applyStateToExecutor(executor)
            }

            executor
        } else {
            throw IllegalStateException("Cannot play the same instance of a track twice, use track.makeClone().")
        }
    }

    override fun stop() {
        activeExecutor.stop()
    }

    override fun setMarker(marker: TrackMarker?) {
        activeExecutor.setMarker(marker)
    }

    override fun provide(): AudioFrame? {
        return activeExecutor.provide()
    }

    @Throws(TimeoutException::class, InterruptedException::class)
    override fun provide(timeout: Long, unit: TimeUnit): AudioFrame? {
        return activeExecutor.provide(timeout, unit)
    }

    override fun provide(targetFrame: MutableAudioFrame): Boolean {
        return activeExecutor.provide(targetFrame)
    }

    @Throws(TimeoutException::class, InterruptedException::class)
    override fun provide(targetFrame: MutableAudioFrame, timeout: Long, unit: TimeUnit): Boolean {
        return activeExecutor.provide(targetFrame, timeout, unit)
    }

    override fun makeClone(): AudioTrack? {
        val track = makeShallowClone()
        track.userData = userData
        return track
    }

    override fun createLocalExecutor(playerManager: AudioPlayerManager?): AudioTrackExecutor? {
        return null
    }

    override fun <T> getUserData(klass: Class<T>?): T? {
        val data = userData
        return if (data != null && klass!!.isAssignableFrom(data.javaClass)) data as? T else null
    }

    protected open fun makeShallowClone(): AudioTrack {
        throw UnsupportedOperationException()
    }
}
