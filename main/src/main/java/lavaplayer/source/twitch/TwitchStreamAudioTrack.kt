package lavaplayer.source.twitch

import lavaplayer.track.AudioTrackInfo
import lavaplayer.source.stream.MpegTsM3uStreamAudioTrack
import lavaplayer.source.stream.M3uStreamSegmentUrlProvider
import lavaplayer.tools.io.HttpInterface
import kotlin.Throws
import lavaplayer.track.playback.LocalAudioTrackExecutor
import lavaplayer.track.AudioTrack
import org.slf4j.LoggerFactory
import java.lang.Exception

/**
 * Audio track that handles processing Twitch tracks.
 *
 * @param trackInfo     Track info
 * @param sourceManager Source manager which was used to find this track
 */
class TwitchStreamAudioTrack(
    trackInfo: AudioTrackInfo?,
    override val sourceManager: TwitchStreamItemSourceManager
) : MpegTsM3uStreamAudioTrack(trackInfo) {
    companion object {
        private val log = LoggerFactory.getLogger(TwitchStreamAudioTrack::class.java)
    }

    private val segmentUrlProvider = TwitchStreamSegmentUrlProvider(channelName, sourceManager)

    /**
     * @return Name of the channel of the stream.
     */
    val channelName: String
        get() = TwitchStreamItemSourceManager.getChannelIdentifierFromUrl(info.identifier)

    override fun getSegmentUrlProvider(): M3uStreamSegmentUrlProvider {
        return segmentUrlProvider
    }

    override fun getHttpInterface(): HttpInterface {
        return sourceManager.httpInterface
    }

    @Throws(Exception::class)
    override fun process(executor: LocalAudioTrackExecutor) {
        log.debug("Starting to play Twitch channel {}.", channelName)
        super.process(executor)
    }

    override fun makeShallowClone(): AudioTrack =
        TwitchStreamAudioTrack(info, sourceManager)

}
