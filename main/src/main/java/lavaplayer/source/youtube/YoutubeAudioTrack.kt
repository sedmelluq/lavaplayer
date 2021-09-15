package lavaplayer.source.youtube

import lavaplayer.container.Formats
import lavaplayer.container.matroska.MatroskaAudioTrack
import lavaplayer.container.mpeg.MpegAudioTrack
import lavaplayer.tools.FriendlyException
import lavaplayer.tools.io.HttpInterface
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackInfo
import lavaplayer.track.DelegatedAudioTrack
import lavaplayer.track.playback.LocalAudioTrackExecutor
import mu.KotlinLogging
import java.net.URI

/**
 * Audio track that handles processing YouTube videos as audio tracks.
 * @param trackInfo     Track info
 * @param sourceManager Source manager which was used to find this track
 */
class YoutubeAudioTrack(trackInfo: AudioTrackInfo?, override val sourceManager: YoutubeItemSourceManager) :
    DelegatedAudioTrack(trackInfo!!) {
    companion object {
        private val log = KotlinLogging.logger { }

        private fun YoutubeTrackFormat.betterThan(other: YoutubeTrackFormat?): Boolean {
            return when {
                info == null -> false
                other == null -> true
                info.ordinal != other.info.ordinal -> info.ordinal < other.info.ordinal
                else -> bitrate > other.bitrate
            }
        }

        private fun findBestSupportedFormat(formats: List<YoutubeTrackFormat>): YoutubeTrackFormat {
            val bestFormat = formats.fold<YoutubeTrackFormat, YoutubeTrackFormat?>(null) { a, n -> if (n.betterThan(a)) n else a }
            check(bestFormat != null) { "No supported audio streams available, available types: ${formats.joinToString { it.type.mimeType }}" }
            return bestFormat
        }
    }

    override fun makeShallowClone(): AudioTrack =
        YoutubeAudioTrack(info, sourceManager)

    @Throws(Exception::class)
    override fun process(executor: LocalAudioTrackExecutor) {
        sourceManager.httpInterface.use { httpInterface ->
            val format = loadBestFormatWithUrl(httpInterface)
            log.debug { "Starting track from URL: $format.signedUrl{}" }

            if (info.isStream) {
                processStream(executor, format)
            } else {
                processStatic(executor, httpInterface, format)
            }
        }
    }

    @Throws(Exception::class)
    private fun processStatic(localExecutor: LocalAudioTrackExecutor, httpInterface: HttpInterface, format: FormatWithUrl) {
        YoutubePersistentHttpStream(httpInterface, format.signedUrl, format.details.contentLength).use { stream ->
            if (format.details.type.mimeType.endsWith("/webm")) {
                processDelegate(MatroskaAudioTrack(info, stream), localExecutor)
            } else {
                processDelegate(MpegAudioTrack(info, stream), localExecutor)
            }
        }
    }

    @Throws(Exception::class)
    private fun processStream(localExecutor: LocalAudioTrackExecutor, format: FormatWithUrl) {
        if (Formats.MIME_AUDIO_WEBM == format.details.type.mimeType) {
            throw FriendlyException("YouTube WebM streams are currently not supported.", FriendlyException.Severity.COMMON, null)
        }

        sourceManager.httpInterface.use { streamingInterface ->
            val track = YoutubeMpegStreamAudioTrack(info, streamingInterface, format.signedUrl)
            processDelegate(track, localExecutor)
        }
    }

    @Throws(Exception::class)
    private fun loadBestFormatWithUrl(httpInterface: HttpInterface): FormatWithUrl {
        val details = sourceManager.trackDetailsLoader.loadDetails(httpInterface, identifier, true, sourceManager)
            ?: throw FriendlyException("This video is not available", FriendlyException.Severity.COMMON, null)

        // If the error reason is "Video unavailable" details will return null
        val formats = details.getFormats(httpInterface, sourceManager.signatureResolver)
        val format = findBestSupportedFormat(formats)
        val signedUrl = sourceManager.signatureResolver.resolveFormatUrl(httpInterface, details.playerScript, format)

        return FormatWithUrl(format, signedUrl)
    }

    data class FormatWithUrl(val details: YoutubeTrackFormat, val signedUrl: URI)
}
