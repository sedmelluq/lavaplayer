package lavaplayer.source.youtube.music

import lavaplayer.tools.http.ExtendedHttpConfigurable
import lavaplayer.track.AudioItem
import lavaplayer.track.AudioTrackFactory

interface YoutubeSearchMusicResultLoader {
    fun loadSearchMusicResult(query: String, trackFactory: AudioTrackFactory): AudioItem

    fun getHttpConfiguration(): ExtendedHttpConfigurable
}
