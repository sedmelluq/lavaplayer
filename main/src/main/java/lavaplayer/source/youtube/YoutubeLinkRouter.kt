package lavaplayer.source.youtube

interface YoutubeLinkRouter {
    fun <T> route(link: String, routes: Routes<T>): T

    interface Routes<T> {
        fun track(videoId: String): T

        fun playlist(playlistId: String, selectedVideoId: String?): T

        fun mix(mixId: String, selectedVideoId: String?): T

        fun search(query: String): T

        fun searchMusic(query: String): T

        fun anonymous(videoIds: String): T

        fun none(): T
    }
}
