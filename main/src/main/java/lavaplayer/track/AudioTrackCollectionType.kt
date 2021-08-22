package lavaplayer.track

import kotlinx.serialization.Serializable

@Serializable
open class AudioTrackCollectionType {
    /**
     * This audio track collection represents a (user) created track collection.
     */
    @Serializable
    object Playlist : AudioTrackCollectionType() {
        override fun toString(): String = "Playlist()"
    }

    /**
     * This audio track collection represents an album.
     */
    @Serializable
    open class Album(val artist: String) : AudioTrackCollectionType() {
        override fun toString() = "Album(artist=$artist)"
    }

    /**
     * This audio track collection represents a search result.
     */
    @Serializable
    open class SearchResult(val query: String) : AudioTrackCollectionType() {
        override fun toString() = "SearchResult(query=$query)"
    }
}
