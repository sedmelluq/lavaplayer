package lavaplayer.container.playlists

import lavaplayer.source.stream.MpegTsM3uStreamAudioTrack
import lavaplayer.tools.io.HttpInterface
import lavaplayer.tools.io.HttpInterfaceManager
import lavaplayer.track.AudioTrackInfo

/**
 * @param trackInfo            Track info
 * @param httpInterfaceManager The http interface manager
 */
class HlsStreamTrack(
    trackInfo: AudioTrackInfo,
    streamUrl: String,
    val httpInterfaceManager: HttpInterfaceManager,
    isInnerUrl: Boolean
) : MpegTsM3uStreamAudioTrack(trackInfo) {
    override val segmentUrlProvider: HlsStreamSegmentUrlProvider = if (isInnerUrl) {
        HlsStreamSegmentUrlProvider(null, streamUrl)
    } else {
        HlsStreamSegmentUrlProvider(streamUrl, null)
    }

    override val httpInterface: HttpInterface
        get() = httpInterfaceManager.get()
}
