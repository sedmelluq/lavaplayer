package com.sedmelluq.discord.lavaplayer.format

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormatTools.toAudioFormat
import com.sedmelluq.discord.lavaplayer.manager.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools
import com.sedmelluq.discord.lavaplayer.track.TrackStateListener
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.sound.sampled.AudioInputStream

/**
 * Provides an audio player as an input stream. When nothing is playing, it returns silence instead of blocking.
 *
 * @param format         Format of the frames expected from the player
 * @param player         The player to read frames from
 * @param timeout        Timeout till track stuck event is sent. Each time a new frame is required from the player, it asks
 * for a frame with the specified timeout. In case that timeout is reached, the track stuck event is
 * sent and if providing silence is enabled, silence is provided as the next frame.
 * @param provideSilence True if the stream should return silence instead of blocking in case nothing is playing or
 * read times out.
 */
class AudioPlayerInputStream(
    private val format: AudioDataFormat,
    private val player: AudioPlayer,
    private val timeout: Long,
    private val provideSilence: Boolean
) : InputStream() {
    companion object {
        /**
         * Create an instance of AudioInputStream using an AudioPlayer as a source.
         *
         * @param player         Format of the frames expected from the player
         * @param format         The player to read frames from
         * @param stuckTimeout   Timeout till track stuck event is sent and silence is returned on reading
         * @param provideSilence Returns true if the stream should provide silence if no track is being played or when getting
         * track frames times out.
         * @return An audio input stream usable with JDK sound system
         */
        @JvmStatic
        fun createStream(
            player: AudioPlayer,
            format: AudioDataFormat,
            stuckTimeout: Long,
            provideSilence: Boolean
        ): AudioInputStream {
            val jdkFormat = toAudioFormat(format)
            return AudioInputStream(AudioPlayerInputStream(format, player, stuckTimeout, provideSilence), jdkFormat, -1)
        }
    }

    private var current: ByteBuffer? = null

    @Throws(IOException::class)
    override fun read(): Int {
        ensureAvailable()
        return current!!.get().toInt()
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        var currentOffset = offset
        while (currentOffset < length) {
            ensureAvailable()
            val piece = current!!.remaining().coerceAtMost(length - currentOffset)
            current!![buffer, currentOffset, piece]
            currentOffset += piece
        }

        return currentOffset - offset
    }

    @Throws(IOException::class)
    override fun available(): Int {
        return if (current != null) current!!.remaining() else 0
    }

    @Throws(IOException::class)
    override fun close() {
        player.stopTrack()
    }

    @Throws(IOException::class)
    private fun ensureAvailable() {
        while (available() == 0) {
            try {
                attemptRetrieveFrame()
            } catch (e: TimeoutException) {
                notifyTrackStuck()
            } catch (e: InterruptedException) {
                ExceptionTools.keepInterrupted(e)
                throw InterruptedIOException()
            }
            if (available() == 0 && provideSilence) {
                addFrame(format.silenceBytes())
                break
            }
        }
    }

    @Throws(TimeoutException::class, InterruptedException::class)
    private fun attemptRetrieveFrame() {
        val frame = player.provide(timeout, TimeUnit.MILLISECONDS)
        if (frame != null) {
            check(format == frame.format) {
                "Frame read from the player uses a different format than expected."
            }

            addFrame(frame.data)
        } else if (!provideSilence) {
            Thread.sleep(10)
        }
    }

    private fun addFrame(data: ByteArray) {
        current = ByteBuffer.wrap(data)
    }

    private fun notifyTrackStuck() {
        if (player is TrackStateListener) {
            player.onTrackStuck(player.playingTrack!!, timeout)
        }
    }
}
