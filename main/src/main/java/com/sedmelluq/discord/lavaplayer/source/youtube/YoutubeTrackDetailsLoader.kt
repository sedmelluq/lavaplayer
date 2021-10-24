package com.sedmelluq.discord.lavaplayer.source.youtube

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface

interface YoutubeTrackDetailsLoader {
    fun loadDetails(
        httpInterface: HttpInterface,
        videoId: String,
        requireFormats: Boolean,
        sourceManager: YoutubeItemSourceManager
    ): YoutubeTrackDetails?
}
