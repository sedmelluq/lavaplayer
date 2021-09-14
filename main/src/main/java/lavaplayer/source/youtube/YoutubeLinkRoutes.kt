package lavaplayer.source.youtube

import lavaplayer.source.common.LinkRoutes
import lavaplayer.track.AudioItem

interface YoutubeLinkRoutes : LinkRoutes {
    fun track(videoId: String): AudioItem?

    fun playlist(playlistId: String, selectedVideoId: String?): AudioItem?

    fun mix(mixId: String, selectedVideoId: String?): AudioItem?

    fun search(query: String): AudioItem?

    fun searchMusic(query: String): AudioItem?

    fun anonymous(videoIds: String): AudioItem?

    fun none(): AudioItem?
}
