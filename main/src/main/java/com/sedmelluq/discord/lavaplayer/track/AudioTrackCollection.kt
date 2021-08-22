package com.sedmelluq.discord.lavaplayer.track

interface AudioTrackCollection : AudioItem {

    /**
     * The name of this audio track collection.
     */
    val name: String

    /**
     * The type of this collection.
     */
    val type: AudioTrackCollectionType

    /**
     * List of tracks in this collection
     */
    val tracks: List<AudioTrack>

    /**
     * The track that is explicitly selected, may be null. This same instance occurs in the track list.
     */
    val selectedTrack: AudioTrack?

}
