package com.sedmelluq.discord.lavaplayer.source.soundcloud

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import java.io.IOException

interface SoundCloudHtmlDataLoader {
    @Throws(IOException::class)
    fun load(httpInterface: HttpInterface, url: String): SoundCloudRootDataModel?
}
