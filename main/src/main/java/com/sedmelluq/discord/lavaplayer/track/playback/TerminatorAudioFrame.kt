package com.sedmelluq.discord.lavaplayer.track.playback

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat

/**
 * Audio frame where [isTerminator] is `true`.
 */
open class TerminatorAudioFrame : AudioFrame {
    companion object INSTANCE : TerminatorAudioFrame()

    override val timecode: Long
        get() = throw UnsupportedOperationException()

    override val volume: Int
        get() = throw UnsupportedOperationException()

    override val dataLength: Int
        get() = throw UnsupportedOperationException()

    override val data: ByteArray
        get() = throw UnsupportedOperationException()

    override val format: AudioDataFormat
        get() = throw UnsupportedOperationException()

    override val isTerminator: Boolean
        get() = true

    override fun getData(buffer: ByteArray, offset: Int) =
        throw UnsupportedOperationException()
}
