package com.sedmelluq.discord.lavaplayer.track

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput
import java.io.*

abstract class DefaultTrackEncoder : TrackEncoder {
    /**
     * The list of enabled source managers.
     */
    abstract val sourceManagers: List<AudioSourceManager>

    @Throws(IOException::class)
    override fun encodeTrack(stream: MessageOutput, track: AudioTrack) {
        val output = stream.startMessage()
        output.apply {
            write(TrackEncoder.TRACK_INFO_VERSION)

            /* encode specific details about the track. */
            AudioTrackInfo.encode(output, track.info)
            encodeTrackDetails(track, output)
            output.writeLong(track.position)
        }

        stream.commitMessage(TrackEncoder.TRACK_INFO_VERSIONED)
    }

    @Throws(IOException::class)
    override fun decodeTrack(stream: MessageInput): DecodedTrackHolder? {
        val input = stream.nextMessage()
            ?: return null

        val version = AudioTrackInfo.getVersion(stream, input)
        val trackInfo = AudioTrackInfo.decode(input, version)
        val track = decodeTrackDetails(trackInfo, input)

        track?.let { it.position = input.readLong() }
        stream.skipRemainingBytes()

        return DecodedTrackHolder(track)
    }

    /**
     * Encodes an audio track to a byte array. Does not include AudioTrackInfo in the buffer.
     *
     * @param track The track to encode
     * @return The bytes of the encoded data
     */
    protected open fun encodeTrackDetails(track: AudioTrack): ByteArray? {
        return try {
            val byteOutput = ByteArrayOutputStream()
            val output: DataOutput = DataOutputStream(byteOutput)
            encodeTrackDetails(track, output)
            byteOutput.toByteArray()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    /**
     * Decodes an audio track from a byte array.
     *
     * @param trackInfo Track info for the track to decode
     * @param buffer    Byte array containing the encoded track
     * @return Decoded audio track
     */
    fun decodeTrackDetails(trackInfo: AudioTrackInfo, buffer: ByteArray?): AudioTrack? {
        return try {
            val input = DataInputStream(ByteArrayInputStream(buffer))
            decodeTrackDetails(trackInfo, input)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class)
    protected open fun encodeTrackDetails(track: AudioTrack, output: DataOutput) {
        val sourceManager = track.sourceManager
        output.writeUTF(sourceManager.sourceName)
        sourceManager.encodeTrack(track, output)
    }

    @Throws(IOException::class)
    protected open fun decodeTrackDetails(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack? {
        val sourceName = input.readUTF()
        return sourceManagers.find { it.sourceName == sourceName }
            ?.decodeTrack(trackInfo, input)
    }

}
