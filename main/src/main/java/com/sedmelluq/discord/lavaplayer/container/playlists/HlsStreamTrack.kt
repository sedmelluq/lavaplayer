package com.sedmelluq.discord.lavaplayer.container.playlists

import com.sedmelluq.discord.lavaplayer.source.stream.MpegTsM3uStreamAudioTrack
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo

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
