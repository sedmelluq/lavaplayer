package com.sedmelluq.discord.lavaplayer.container.ogg

import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoProvider
import com.sedmelluq.discord.lavaplayer.container.ogg.OggMetadata
import com.sedmelluq.discord.lavaplayer.tools.Units

/**
 * Audio track info provider based on OGG metadata map.
 *
 * @param tags Map of OGG metadata with OGG-specific keys.
 */
class OggMetadata(private val tags: Map<String, String>, override val length: Long?) : AudioTrackInfoProvider {
    companion object {
        @JvmField
        val EMPTY = OggMetadata(emptyMap(), Units.DURATION_MS_UNKNOWN)
        private const val TITLE_FIELD = "TITLE"
        private const val ARTIST_FIELD = "ARTIST"
    }

    override val title: String?
        get() = tags[TITLE_FIELD]

    override val author: String?
        get() = tags[ARTIST_FIELD]

    override val identifier: String?
        get() = null

    override val uri: String?
        get() = null

    override val artworkUrl: String?
        get() = null
}
