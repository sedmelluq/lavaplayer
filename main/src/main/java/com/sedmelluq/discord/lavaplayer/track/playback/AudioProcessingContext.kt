package com.sedmelluq.discord.lavaplayer.track.playback

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.manager.AudioConfiguration
import com.sedmelluq.discord.lavaplayer.manager.AudioPlayerResources

/**
 * Context for processing audio. Contains configuration for encoding and the output where the frames go to.
 *
 * @param configuration Audio encoding or filtering related configuration
 * @param frameBuffer   Frame buffer for the produced audio frames
 * @param playerOptions State of the audio player.
 * @param outputFormat  Output format to use throughout this processing cycle
 */
data class AudioProcessingContext(
    /**
     * Audio encoding or filtering related configuration
     */
    @JvmField
    val configuration: AudioConfiguration,
    /**
     * Consumer for the produced audio frames
     */
    @JvmField
    val frameBuffer: AudioFrameBuffer,
    /**
     * Mutable volume level for the audio
     */
    @JvmField
    val playerOptions: AudioPlayerResources,
    /**
     * Output format to use throughout this processing cycle
     */
    @JvmField
    val outputFormat: AudioDataFormat
) {
    /**
     * Whether filter factory change is applied to already playing tracks.
     */
    val filterHotSwapEnabled: Boolean
        @JvmName("isFilterHotSwapEnabled")
        get() = configuration.filterHotSwapEnabled
}
