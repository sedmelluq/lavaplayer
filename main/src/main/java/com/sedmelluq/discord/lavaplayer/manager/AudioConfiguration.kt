package com.sedmelluq.discord.lavaplayer.manager

import com.sedmelluq.discord.lavaplayer.filter.ResamplingQuality
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.track.playback.AllocatingAudioFrameBuffer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameBufferFactory

/**
 * Audio processing configuration.
 */
class AudioConfiguration {
    companion object {
        const val OPUS_QUALITY_MAX = 10
    }

    @get:JvmName("isFilterHotSwapEnabled")
    var filterHotSwapEnabled: Boolean = false
    var resamplingQuality: ResamplingQuality = ResamplingQuality.LOW
    var outputFormat: AudioDataFormat = StandardAudioDataFormats.DISCORD_OPUS
    var opusEncodingQuality: Int = OPUS_QUALITY_MAX
        set(value) {
            field = value.coerceIn(0..OPUS_QUALITY_MAX)
        }

    lateinit var frameBufferFactory: AudioFrameBufferFactory

    init {
        useFrameBufferFactory(::AllocatingAudioFrameBuffer)
    }

    operator fun invoke(block: AudioConfiguration.() -> Unit): AudioConfiguration {
        return apply(block)
    }

    fun useFrameBufferFactory(factory: AudioFrameBufferFactory): AudioConfiguration {
        frameBufferFactory = factory
        return this
    }

    fun clone(): AudioConfiguration {
        val copy = AudioConfiguration()
        copy.resamplingQuality = resamplingQuality
        copy.opusEncodingQuality = opusEncodingQuality
        copy.outputFormat = outputFormat
        copy.filterHotSwapEnabled = filterHotSwapEnabled
        copy.frameBufferFactory = frameBufferFactory
        return copy
    }
}
