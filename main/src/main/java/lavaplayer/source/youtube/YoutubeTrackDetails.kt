package lavaplayer.source.youtube

import lavaplayer.tools.io.HttpInterface
import lavaplayer.track.AudioTrackInfo

interface YoutubeTrackDetails {
    fun getTrackInfo(): AudioTrackInfo

    fun getFormats(httpInterface: HttpInterface, signatureResolver: YoutubeSignatureResolver): List<YoutubeTrackFormat>

    fun getPlayerScript(): String?
}
