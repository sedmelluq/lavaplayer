package com.sedmelluq.discord.lavaplayer.track

interface AudioTrackCollectionType {
    /**
     * This audio track collection represents a (user) created track collection.
     */
    object Playlist : AudioTrackCollectionType {
        override fun toString(): String = "Playlist()"
    }

    /**
     * This audio track collection represents an album.
     */
    open class Album(val artist: String) : AudioTrackCollectionType {
        override fun toString() = "Album(artist=$artist)"
    }

    /**
     * This audio track collection represents a search result.
     */
    open class SearchResult(val query: String) : AudioTrackCollectionType {
        override fun toString() = "SearchResult(query=$query)"
    }
}
