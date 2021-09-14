package lavaplayer.container

import lavaplayer.tools.io.SeekableInputStream
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackInfo

data class MediaContainerDescriptor(val probe: MediaContainerProbe, val parameters: String?) {
    fun createTrack(trackInfo: AudioTrackInfo, inputStream: SeekableInputStream): AudioTrack {
        return probe.createTrack(parameters, trackInfo, inputStream)
    }
}
