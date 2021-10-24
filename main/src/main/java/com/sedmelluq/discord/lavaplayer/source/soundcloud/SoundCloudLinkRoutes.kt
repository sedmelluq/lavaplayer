package com.sedmelluq.discord.lavaplayer.source.soundcloud

import com.sedmelluq.discord.lavaplayer.source.common.LinkRoutes
import com.sedmelluq.discord.lavaplayer.track.AudioItem

interface SoundCloudLinkRoutes : LinkRoutes {
    fun track(url: String): AudioItem?

    fun liked(url: String): AudioItem?

    fun set(url: String): AudioItem?

    fun search(query: String, offset: Int, limit: Int): AudioItem?
}
