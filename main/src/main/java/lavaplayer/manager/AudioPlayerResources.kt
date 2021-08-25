package lavaplayer.manager

import lavaplayer.filter.PcmFilterFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Mutable resources of an audio player which may be applied in real-time.
 */
class AudioPlayerResources {
    /**
     * Volume level of the audio, see {@link AudioPlayer#setVolume(int)}. Applied in real-time.
     */
    @JvmField
    val volumeLevel = AtomicInteger(100)

    /**
     * Current PCM filter factory. Applied in real-time.
     */
    @JvmField
    val filterFactory = AtomicReference<PcmFilterFactory>()

    /**
     * Current frame buffer size. If not set, the global default is used. Changing this only affects the next track that
     * is started.
     */
    @JvmField
    val frameBufferDuration = AtomicReference<Int>()
}
