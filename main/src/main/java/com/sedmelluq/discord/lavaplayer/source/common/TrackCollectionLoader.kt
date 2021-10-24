package com.sedmelluq.discord.lavaplayer.source.common

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager
import com.sedmelluq.discord.lavaplayer.track.AudioTrackCollection
import com.sedmelluq.discord.lavaplayer.track.AudioTrackFactory

interface TrackCollectionLoader {
    /**
     * Loads a new audio track collection with the provided [identifier]
     *
     * @param identifier The identifier to use
     * @param httpInterfaceManager
     * @param trackFactory
     */
    fun load(
        identifier: String,
        httpInterfaceManager: HttpInterfaceManager,
        trackFactory: AudioTrackFactory
    ): AudioTrackCollection?
}
