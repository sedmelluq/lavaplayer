package com.sedmelluq.discord.lavaplayer.source.youtube

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import com.sedmelluq.discord.lavaplayer.track.AudioTrackCollection
import com.sedmelluq.discord.lavaplayer.track.AudioTrackFactory

interface YoutubePlaylistLoader {
    var playlistPageCount: Int

    fun load(
        httpInterface: HttpInterface,
        playlistId: String,
        selectedVideoId: String?,
        trackFactory: AudioTrackFactory
    ): AudioTrackCollection
}
