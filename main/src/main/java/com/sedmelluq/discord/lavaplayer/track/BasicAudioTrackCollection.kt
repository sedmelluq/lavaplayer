package com.sedmelluq.discord.lavaplayer.track

import kotlinx.serialization.Serializable

@Serializable
open class BasicAudioTrackCollection(
    override val name: String,
    override val type: AudioTrackCollectionType,
    override val tracks: MutableList<AudioTrack>,
    override val selectedTrack: AudioTrack? = null
) : AudioTrackCollection
