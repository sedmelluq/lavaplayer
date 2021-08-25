package lavaplayer.source.youtube.music

import lavaplayer.tools.http.ExtendedHttpConfigurable
import lavaplayer.track.AudioItem
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackInfo
import java.util.function.Function

interface YoutubeSearchMusicResultLoader {
    fun loadSearchMusicResult(query: String, trackFactory: Function<AudioTrackInfo, AudioTrack>): AudioItem

    fun getHttpConfiguration(): ExtendedHttpConfigurable
}
