package com.sedmelluq.discord.lavaplayer.manager

import kotlinx.atomicfu.atomic
import com.sedmelluq.discord.lavaplayer.filter.PcmFilterFactory

/**
 * Mutable resources of an audio player which may be applied in real-time.
 */
class AudioPlayerResources {
    private val _volumeLevel = atomic(100)

    /**
     * Volume level of the audio, see {@link AudioPlayer#setVolume(int)}. Applied in real-time.
     */
    var volumeLevel: Int by _volumeLevel

    private val _filterFactory = atomic<PcmFilterFactory?>(null)

    /**
     * Current PCM filter factory. Applied in real-time.
     */
    var filterFactory: PcmFilterFactory? by _filterFactory

    private val _frameBufferDuration = atomic(0)

    /**
     * Current frame buffer size. If not set, the global default is used. Changing this only affects the next track that
     * is started.
     */
    var frameBufferDuration: Int by _frameBufferDuration
}
