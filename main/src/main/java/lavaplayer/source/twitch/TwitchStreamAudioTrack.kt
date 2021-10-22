package lavaplayer.source.twitch

import lavaplayer.source.stream.MpegTsM3uStreamAudioTrack
import lavaplayer.tools.io.HttpInterface
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackInfo
import lavaplayer.track.playback.LocalAudioTrackExecutor
import mu.KotlinLogging

/**
 * Audio track that handles processing Twitch tracks.
 *
 * @param trackInfo     Track info
 * @param sourceManager Source manager which was used to find this track
 */
class TwitchStreamAudioTrack(
    trackInfo: AudioTrackInfo,
    override val sourceManager: TwitchStreamItemSourceManager
) : MpegTsM3uStreamAudioTrack(trackInfo) {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    /**
     * @return Name of the channel of the stream.
     */
    val channelName: String?
        get() = TwitchStreamItemSourceManager.getChannelIdentifierFromUrl(info.identifier)

    override val segmentUrlProvider = TwitchStreamSegmentUrlProvider(channelName, sourceManager)

    override val httpInterface: HttpInterface
        get() = sourceManager.httpInterface

    @Throws(Exception::class)
    override fun process(executor: LocalAudioTrackExecutor) {
        log.debug { "Starting to play Twitch channel $channelName." }
        super.process(executor)
    }

    override fun makeShallowClone(): AudioTrack =
        TwitchStreamAudioTrack(info, sourceManager)

}
