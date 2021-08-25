package lavaplayer.source.youtube.format

import lavaplayer.source.youtube.YoutubeSignatureResolver
import lavaplayer.source.youtube.YoutubeTrackFormat
import lavaplayer.source.youtube.YoutubeTrackJsonData
import lavaplayer.tools.io.HttpInterface

interface YoutubeTrackFormatExtractor {
    companion object {
        const val DEFAULT_SIGNATURE_KEY = "signature"
    }

    fun extract(
        response: YoutubeTrackJsonData,
        httpInterface: HttpInterface,
        signatureResolver: YoutubeSignatureResolver
    ): List<YoutubeTrackFormat>
}
