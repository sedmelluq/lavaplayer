package com.sedmelluq.discord.lavaplayer.source.youtube.music

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import com.sedmelluq.discord.lavaplayer.track.AudioTrackCollection
import com.sedmelluq.discord.lavaplayer.track.AudioTrackFactory

interface YoutubeMixLoader {
    fun load(
        httpInterface: HttpInterface,
        mixId: String,
        selectedVideoId: String?,
        trackFactory: AudioTrackFactory
    ): AudioTrackCollection
}
