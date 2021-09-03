package lavaplayer.track

import kotlinx.serialization.Serializable

// TODO: think of a better way of doing this...

interface AudioTrackCollectionType {
    /**
     * This audio track collection represents a (user) created track collection.
     */
    @Serializable
    object Playlist : AudioTrackCollectionType

    /**
     * This audio track collection represents an album.
     */
    @Serializable
    open class Album(val artist: String) : AudioTrackCollectionType

    /**
     * This audio track collection represents a search result.
     */
    @Serializable
    open class SearchResult(val query: String) : AudioTrackCollectionType
}
