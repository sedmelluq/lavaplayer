package lavaplayer.source

import lavaplayer.tools.io.HttpInterfaceManager
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackCollection
import lavaplayer.track.AudioTrackInfo
import java.util.function.Function

interface TrackCollectionLoader {
    fun load(
        identifier: String,
        httpInterfaceManager: HttpInterfaceManager,
        trackFactory: Function<AudioTrackInfo, AudioTrack>
    ): AudioTrackCollection?
}
