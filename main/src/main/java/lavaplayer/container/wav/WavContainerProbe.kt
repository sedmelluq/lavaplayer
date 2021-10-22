package lavaplayer.container.wav

import lavaplayer.container.MediaContainerDetection
import lavaplayer.container.MediaContainerDetectionResult
import lavaplayer.container.MediaContainerHints
import lavaplayer.container.MediaContainerProbe
import lavaplayer.tools.io.SeekableInputStream
import lavaplayer.track.AudioReference
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackInfo
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
