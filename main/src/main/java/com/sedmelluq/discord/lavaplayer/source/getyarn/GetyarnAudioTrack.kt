package com.sedmelluq.discord.lavaplayer.source.getyarn

import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack
import com.sedmelluq.discord.lavaplayer.tools.Units
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import mu.KotlinLogging
import java.net.URI

class GetyarnAudioTrack(trackInfo: AudioTrackInfo, override val sourceManager: GetyarnItemSourceManager) :
    DelegatedAudioTrack(trackInfo) {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    override fun process(executor: LocalAudioTrackExecutor) {
        sourceManager.httpInterface.use { httpInterface ->
            log.debug { "Starting getyarn.io track from URL: ${info.identifier}" }
            PersistentHttpStream(httpInterface, URI(info.identifier), Units.CONTENT_LENGTH_UNKNOWN).use { stream ->
                processDelegate(MpegAudioTrack(info, stream), executor)
            }
        }
    }

    override fun makeShallowClone() =
        GetyarnAudioTrack(info, sourceManager)
}
