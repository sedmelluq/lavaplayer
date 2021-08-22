package com.sedmelluq.discord.lavaplayer.source.youtube

import com.sedmelluq.discord.lavaplayer.source.youtube.format.LegacyAdaptiveFormatsExtractor
import com.sedmelluq.discord.lavaplayer.source.youtube.format.LegacyDashMpdFormatsExtractor
import com.sedmelluq.discord.lavaplayer.source.youtube.format.LegacyStreamMapFormatsExtractor
import com.sedmelluq.discord.lavaplayer.source.youtube.format.StreamingDataFormatsExtractor
import com.sedmelluq.discord.lavaplayer.tools.*
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS
import com.sedmelluq.discord.lavaplayer.tools.Units.DURATION_MS_UNKNOWN
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DefaultYoutubeTrackDetails(
    private val videoId: String,
    private val data: YoutubeTrackJsonData
) : YoutubeTrackDetails {

    companion object {
        val log: Logger = LoggerFactory.getLogger(DefaultYoutubeTrackDetails::class.java)
        val FORMAT_EXTRACTORS = listOf(
            LegacyAdaptiveFormatsExtractor(),
            StreamingDataFormatsExtractor(),
            LegacyDashMpdFormatsExtractor(),
            LegacyStreamMapFormatsExtractor()
        )
    }

    override fun getTrackInfo(): AudioTrackInfo =
        loadTrackInfo()

    override fun getPlayerScript(): String =
        data.playerScriptUrl

    override fun getFormats(
        httpInterface: HttpInterface,
        signatureResolver: YoutubeSignatureResolver
    ): List<YoutubeTrackFormat> {
        try {
            return loadTrackFormats(httpInterface, signatureResolver)
        } catch (e: Exception) {
            throw ExceptionTools.toRuntimeException(e)
        }
    }

    private fun loadTrackFormats(
        httpInterface: HttpInterface,
        signatureResolver: YoutubeSignatureResolver
    ): List<YoutubeTrackFormat> {
        for (extractor in FORMAT_EXTRACTORS) {
            val formats = extractor.extract(data, httpInterface, signatureResolver)
            if (formats.isNotEmpty()) {
                return formats
            }
        }

        log.warn("Video $videoId with no detected format field, response ${data.playerResponse.format()} polymer ${data.polymerArguments.format()}")
        throw FriendlyException(
            "Unable to play this YouTube track.",
            SUSPICIOUS,
            IllegalStateException("No track formats found.")
        )
    }

    private fun loadTrackInfo(): AudioTrackInfo {
        val playabilityStatus = data.playerResponse["playabilityStatus"]
        if ("ERROR" == playabilityStatus["status"].safeText) {
            throw FriendlyException(playabilityStatus["reason"].text, COMMON, null)
        }

        val videoDetails = data.playerResponse["videoDetails"].takeUnless { it.isNull }
            ?: return loadLegacyTrackInfo()

        val temporalInfo =
            TemporalInfo.fromRawData(videoDetails["isLiveContent"].asBoolean(false), videoDetails["lengthSeconds"])
        val artworkUrl = ThumbnailTools.extractYouTube(videoDetails, videoId)

        return buildTrackInfo(
            videoId,
            videoDetails["title"].safeText,
            videoDetails["author"].safeText,
            temporalInfo,
            artworkUrl
        )
    }

    private fun loadLegacyTrackInfo(): AudioTrackInfo {
        val args = data.polymerArguments
        if ("fail" == args["status"].safeText) {
            throw FriendlyException(args["reason"].safeText, COMMON, null)
        }

        val temporalInfo = TemporalInfo.fromRawData(args["live_playback"].safeText == "1", args["length_seconds"])
        val artworkUrl = ThumbnailTools.extractYouTube(args, videoId)

        return buildTrackInfo(videoId, args["title"].safeText, args["author"].safeText, temporalInfo, artworkUrl)
    }

    private fun buildTrackInfo(
        videoId: String,
        title: String,
        uploader: String,
        temporalInfo: TemporalInfo,
        artworkUrl: String
    ): AudioTrackInfo {
        return AudioTrackInfo(
            title,
            uploader,
            temporalInfo.durationMillis,
            videoId,
            temporalInfo.isActiveStream,
            "https://www.youtube.com/watch?v=$videoId",
            artworkUrl
        )
    }

    data class TemporalInfo(val isActiveStream: Boolean, val durationMillis: Long) {
        companion object {
            fun fromRawData(wasLiveStream: Boolean, durationSecondsField: JsonBrowser): TemporalInfo {
                val durationValue = durationSecondsField.asLong(0L)
                // VODs are not really live streams, even though that field in JSON claims they are. If it is actually live, then
                // duration is also missing or 0.
                val isActiveStream = wasLiveStream && durationValue == 0L

                return TemporalInfo(
                    isActiveStream,
                    DURATION_MS_UNKNOWN.takeIf { durationValue == 0L } ?: Units.secondsToMillis(durationValue))
            }
        }
    }
}
