package lavaplayer.manager

import kotlinx.atomicfu.atomic
import lavaplayer.filter.PcmFilterFactory

/**
 * Mutable resources of an audio player which may be applied in real-time.
 */
class AudioPlayerResources {
    /**
     * Volume level of the audio, see {@link AudioPlayer#setVolume(int)}. Applied in real-time.
     */
    var volumeLevel by atomic<Int>(100)

    /**
     * Current PCM filter factory. Applied in real-time.
     */
    var filterFactory by atomic<PcmFilterFactory?>(null)

    /**
     * Current frame buffer size. If not set, the global default is used. Changing this only affects the next track that
     * is started.
     */
    var frameBufferDuration by atomic<Int>(0)
}
