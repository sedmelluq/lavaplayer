package lavaplayer.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.*
import lavaplayer.filter.PcmFilterFactory
import lavaplayer.manager.event.AudioEvent
import lavaplayer.track.AudioTrack
import lavaplayer.track.playback.AudioFrameProvider
import mu.KotlinLogging

/**
 * An audio player that is capable of playing audio tracks and provides audio frames from the currently playing track.
 */
interface AudioPlayer : AudioFrameProvider, CoroutineScope {
    /**
     * @return Currently playing track.
     */
    val playingTrack: AudioTrack?

    /**
     * The current volume of this player.
     */
    var volume: Int

    /**
     * Whether the player is paused.
     */
    var isPaused: Boolean

    /**
     * The event flow for this player.
     */
    val events: SharedFlow<AudioEvent>

    /**
     * @param track The track to start playing
     */
    fun playTrack(track: AudioTrack?)

    /**
     * @param track       The track to start playing, passing null will stop the current track and return false
     * @param noInterrupt Whether to only start if nothing else is playing
     * @return True if the track was started
     */
    fun startTrack(track: AudioTrack?, noInterrupt: Boolean): Boolean

    /**
     * Stop currently playing track.
     */
    fun stopTrack()

    /**
     * Configures the filter factory for this player.
     */
    fun setFilterFactory(factory: PcmFilterFactory?)

    /**
     * Sets the frame buffer duration for this player.
     */
    fun setFrameBufferDuration(duration: Int)

    /**
     * Destroy the player and stop playing track.
     */
    fun destroy()

    /**
     * Check if the player should be "cleaned up" - stopped due to nothing using it, with the given threshold.
     *
     * @param threshold Threshold in milliseconds to use
     */
    fun checkCleanup(threshold: Long)
}

@PublishedApi
internal val playerOnLogger = KotlinLogging.logger("AudioPlayer.on")

/**
 *
 */
suspend inline fun <reified T : AudioEvent> AudioPlayer.on(scope: CoroutineScope = this, noinline block: suspend T.() -> Unit): Job {
    return events
        .buffer(UNLIMITED)
        .filterIsInstance<T>()
        .onEach { event ->
            event
                .runCatching { block() }
                .onFailure { playerOnLogger.error(it) { "[${T::class.simpleName}]" } }
        }
        .launchIn(scope)
}
