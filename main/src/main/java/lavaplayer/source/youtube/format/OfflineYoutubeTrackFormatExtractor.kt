package lavaplayer.source.youtube.format

import lavaplayer.source.youtube.YoutubeSignatureResolver
import lavaplayer.source.youtube.YoutubeTrackFormat
import lavaplayer.source.youtube.YoutubeTrackJsonData
import lavaplayer.tools.io.HttpInterface

abstract class OfflineYoutubeTrackFormatExtractor : YoutubeTrackFormatExtractor {
    abstract fun extract(data: YoutubeTrackJsonData): List<YoutubeTrackFormat>

    override fun extract(
        response: YoutubeTrackJsonData,
        httpInterface: HttpInterface,
        signatureResolver: YoutubeSignatureResolver
    ): List<YoutubeTrackFormat> =
        extract(response)
}
