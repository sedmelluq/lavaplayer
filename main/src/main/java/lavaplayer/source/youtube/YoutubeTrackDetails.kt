package lavaplayer.source.youtube

import lavaplayer.tools.io.HttpInterface
import lavaplayer.track.AudioTrackInfo

interface YoutubeTrackDetails {
    val trackInfo: AudioTrackInfo
    val playerScript: String?

    fun getFormats(httpInterface: HttpInterface, signatureResolver: YoutubeSignatureResolver): List<YoutubeTrackFormat>
}
