package com.sedmelluq.discord.lavaplayer.container.ogg

interface OggTrackBlueprint {
    fun loadTrackHandler(stream: OggPacketInputStream): OggTrackHandler
}
