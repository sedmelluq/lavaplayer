package com.sedmelluq.discord.lavaplayer.tools.extensions

import com.sedmelluq.discord.lavaplayer.track.AudioTrackCollection
import com.sedmelluq.discord.lavaplayer.track.AudioTrackCollectionType


val AudioTrackCollection.isPlaylist: Boolean
    get() = type is AudioTrackCollectionType.Playlist

val AudioTrackCollection.isSearchResult: Boolean
    get() = type is AudioTrackCollectionType.SearchResult

val AudioTrackCollection.isAlbum: Boolean
    get() = type is AudioTrackCollectionType.Album
