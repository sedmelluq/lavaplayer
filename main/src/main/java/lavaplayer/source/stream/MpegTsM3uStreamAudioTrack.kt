package lavaplayer.source.stream

import lavaplayer.container.adts.AdtsAudioTrack
import lavaplayer.container.mpegts.MpegTsElementaryInputStream
import lavaplayer.container.mpegts.PesPacketInputStream
import lavaplayer.track.AudioTrackInfo
import lavaplayer.track.playback.LocalAudioTrackExecutor
import java.io.InputStream

/**
 * @param trackInfo Track info
 */
abstract class MpegTsM3uStreamAudioTrack(trackInfo: AudioTrackInfo) : M3uStreamAudioTrack(trackInfo) {
    @Throws(Exception::class)
    override fun processJoinedStream(localExecutor: LocalAudioTrackExecutor, stream: InputStream) {
        val elementaryInputStream = MpegTsElementaryInputStream(stream, MpegTsElementaryInputStream.ADTS_ELEMENTARY_STREAM)
        val pesPacketInputStream = PesPacketInputStream(elementaryInputStream)
        processDelegate(AdtsAudioTrack(info, pesPacketInputStream), localExecutor)
    }
}
