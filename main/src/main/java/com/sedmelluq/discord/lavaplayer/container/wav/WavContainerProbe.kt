package com.sedmelluq.discord.lavaplayer.container.wav

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints
import com.sedmelluq.discord.lavaplayer.container.MediaContainerProbe
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import mu.KotlinLogging
import java.io.IOException

/**
 * Container detection probe for WAV format.
 */
class WavContainerProbe : MediaContainerProbe {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    override val name: String
        get() = "wav"

    override fun matchesHints(hints: MediaContainerHints?): Boolean {
        return false
    }

    @Throws(IOException::class)
    override fun probe(reference: AudioReference, inputStream: SeekableInputStream): MediaContainerDetectionResult? {
        if (!MediaContainerDetection.checkNextBytes(inputStream, WavFileLoader.WAV_RIFF_HEADER)) {
            return null
        }

        log.debug { "Track ${reference.identifier} is a WAV file." }
        val trackInfo = AudioTrackInfo(
            title = reference.title ?: MediaContainerDetection.UNKNOWN_TITLE,
            author = MediaContainerDetection.UNKNOWN_ARTIST,
            length = WavFileLoader(inputStream).parseHeaders().duration,
            identifier = reference.identifier!!,
            uri = reference.identifier,
        )

        return MediaContainerDetectionResult.supportedFormat(this, null, trackInfo)
    }

    override fun createTrack(
        parameters: String?,
        trackInfo: AudioTrackInfo,
        inputStream: SeekableInputStream
    ): AudioTrack {
        return WavAudioTrack(trackInfo, inputStream)
    }
}
