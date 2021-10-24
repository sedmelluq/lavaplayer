package com.sedmelluq.discord.lavaplayer.source.youtube

import com.sedmelluq.discord.lavaplayer.source.common.LinkRoutes
import com.sedmelluq.discord.lavaplayer.track.AudioItem

interface YoutubeLinkRoutes : LinkRoutes {
    fun track(videoId: String): AudioItem?

    fun playlist(playlistId: String, selectedVideoId: String?): AudioItem?

    fun mix(mixId: String, selectedVideoId: String?): AudioItem?

    fun search(query: String): AudioItem?

    fun searchMusic(query: String): AudioItem?

    fun anonymous(videoIds: String): AudioItem?

    fun none(): AudioItem?
}
