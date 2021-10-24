package com.sedmelluq.discord.lavaplayer.container

import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo

data class MediaContainerDescriptor(val probe: MediaContainerProbe, val parameters: String?) {
    fun createTrack(trackInfo: AudioTrackInfo, inputStream: SeekableInputStream): AudioTrack {
        return probe.createTrack(parameters, trackInfo, inputStream)
    }
}
