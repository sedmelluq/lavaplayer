package lavaplayer.tools.extensions

import lavaplayer.track.AudioTrackCollection
import lavaplayer.track.AudioTrackCollectionType


val AudioTrackCollection.isPlaylist: Boolean
    get() = type is AudioTrackCollectionType.Playlist

val AudioTrackCollection.isSearchResult: Boolean
    get() = type is AudioTrackCollectionType.SearchResult

val AudioTrackCollection.isAlbum: Boolean
    get() = type is AudioTrackCollectionType.Album
