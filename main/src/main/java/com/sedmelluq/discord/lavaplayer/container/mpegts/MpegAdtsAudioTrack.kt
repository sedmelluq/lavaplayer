package com.sedmelluq.discord.lavaplayer.container.mpegts

import com.sedmelluq.discord.lavaplayer.container.adts.AdtsAudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import java.io.InputStream

/**
 * @param trackInfo Track info
 */
class MpegAdtsAudioTrack(trackInfo: AudioTrackInfo, private val inputStream: InputStream) : DelegatedAudioTrack(trackInfo) {
    @Throws(Exception::class)
    override fun process(executor: LocalAudioTrackExecutor) {
        val elementaryInputStream = MpegTsElementaryInputStream(inputStream, MpegTsElementaryInputStream.ADTS_ELEMENTARY_STREAM)
        val pesPacketInputStream = PesPacketInputStream(elementaryInputStream)
        processDelegate(AdtsAudioTrack(info, pesPacketInputStream), executor)
    }
}
