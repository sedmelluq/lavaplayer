package com.sedmelluq.discord.lavaplayer.track.playback

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat

/**
 * A single audio frame.
 *
 * @param timecode Timecode of this frame in milliseconds.
 * @param data     Buffer for this frame, in the format specified in the format field.
 * @param volume   Volume level of the audio in this frame.
 * @param format   Specifies the format of audio in the data buffer.
 */
class ImmutableAudioFrame(
    /**
     * Timecode of this frame in milliseconds.
     */
    override val timecode: Long,
    /**
     * Buffer for this frame, in the format specified in the format field.
     */
    override val data: ByteArray,
    /**
     * Volume level of the audio in this frame. Internally when this value is 0, the data may actually contain a
     * non-silent frame. This is to allow frames with 0 volume to be modified later. These frames should still be
     * handled as silent frames.
     */
    override val volume: Int,
    /**
     * Specifies the format of audio in the data buffer.
     */
    override val format: AudioDataFormat
) : AudioFrame {
    override val isTerminator: Boolean
        get() = false

    override val dataLength: Int
        get() = data.size

    override fun getData(buffer: ByteArray, offset: Int) {
        System.arraycopy(data, 0, buffer, offset, data.size)
    }
}
