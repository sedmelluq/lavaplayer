package com.sedmelluq.discord.lavaplayer.source.youtube

import com.sedmelluq.discord.lavaplayer.tools.http.ExtendedHttpConfigurable
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioTrackFactory

interface YoutubeSearchResultLoader {
    val httpConfiguration: ExtendedHttpConfigurable

    fun loadSearchResult(query: String, trackFactory: AudioTrackFactory): AudioItem
}
