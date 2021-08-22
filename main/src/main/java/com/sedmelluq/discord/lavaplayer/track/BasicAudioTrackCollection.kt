package com.sedmelluq.discord.lavaplayer.track

open class BasicAudioTrackCollection(
    override val name: String,
    override val type: AudioTrackCollectionType,
    override val tracks: List<AudioTrack>,
    override val selectedTrack: AudioTrack? = null
) : AudioTrackCollection
