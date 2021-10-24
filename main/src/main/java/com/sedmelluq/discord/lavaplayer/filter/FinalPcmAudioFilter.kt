package com.sedmelluq.discord.lavaplayer.filter

import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import mu.KotlinLogging
import java.nio.ShortBuffer
import java.lang.InterruptedException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * Collects buffers of the required chunk size and passes them on to audio post processors.
 *
 * @param context        Configuration and output information for processing
 * @param postProcessors Post processors to pass the final audio buffers to
 */
class FinalPcmAudioFilter(context: AudioProcessingContext, private val postProcessors: Collection<AudioPostProcessor>) :
    UniversalPcmAudioFilter {
    companion object {
        private val log = KotlinLogging.logger { }
        private val zeroPadding = ShortArray(128)
    }

    private val format: AudioDataFormat = context.outputFormat
    private val frameBuffer: ShortBuffer = ByteBuffer
        .allocateDirect(format.totalSampleCount() * 2)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()
    private var ignoredFrames: Long = 0
    private var timecodeBase: Long = 0
    private var timecodeSampleOffset: Long = 0

    private fun decodeSample(sample: Float): Short {
        return min(max((sample * 32768f).toInt(), -32768), 32767).toShort()
    }

    override fun seekPerformed(requestedTime: Long, providedTime: Long) {
        frameBuffer.clear()
        ignoredFrames = if (requestedTime > providedTime) (requestedTime - providedTime) * format.channelCount * format.sampleRate / 1000L else 0
        timecodeBase = max(requestedTime, providedTime)
        timecodeSampleOffset = 0
        if (ignoredFrames > 0) {
            log.debug { "Ignoring $ignoredFrames frames due to inaccurate seek (requested $requestedTime, provided $providedTime)." }
        }
    }

    @Throws(InterruptedException::class)
    override fun flush() {
        if (frameBuffer.position() > 0) {
            fillFrameBuffer()
            dispatch()
        }
    }

    override fun close() =
        postProcessors.forEach(AudioPostProcessor::close)

    private fun fillFrameBuffer() {
        while (frameBuffer.remaining() >= zeroPadding.size) {
            frameBuffer.put(zeroPadding)
        }

        while (frameBuffer.remaining() > 0) {
            frameBuffer.put(0)
        }
    }

    @Throws(InterruptedException::class)
    override fun process(input: ShortArray, offset: Int, length: Int) {
        for (i in 0 until length) {
            if (ignoredFrames > 0) {
                ignoredFrames--
            } else {
                frameBuffer.put(input[offset + i])
                dispatch()
            }
        }
    }

    @Throws(InterruptedException::class)
    override fun process(input: Array<ShortArray>, offset: Int, length: Int) {
        val secondChannelIndex = min(1, input.size - 1)
        for (i in 0 until length) {
            if (ignoredFrames > 0) {
                ignoredFrames -= format.channelCount.toLong()
            } else {
                frameBuffer.put(input[0][offset + i])
                frameBuffer.put(input[secondChannelIndex][offset + i])
                dispatch()
            }
        }
    }

    @Throws(InterruptedException::class)
    override fun process(buffer: ShortBuffer) {
        if (ignoredFrames > 0) {
            val skipped = min(buffer.remaining().toLong(), ignoredFrames)
            buffer.position(buffer.position() + skipped.toInt())
            ignoredFrames -= skipped
        }

        val local = buffer.duplicate()
        while (buffer.remaining() > 0) {
            val chunk = min(buffer.remaining(), frameBuffer.remaining())
            local.position(buffer.position())
            local.limit(local.position() + chunk)

            frameBuffer.put(local)
            dispatch()

            buffer.position(buffer.position() + chunk)
        }
    }

    @Throws(InterruptedException::class)
    override fun process(input: Array<FloatArray>, offset: Int, length: Int) {
        val secondChannelIndex = min(1, input.size - 1)
        for (i in 0 until length) {
            if (ignoredFrames > 0) {
                ignoredFrames -= 2
            } else {
                frameBuffer.put(decodeSample(input[0][offset + i]))
                frameBuffer.put(decodeSample(input[secondChannelIndex][offset + i]))
                dispatch()
            }
        }
    }

    @Throws(InterruptedException::class)
    private fun dispatch() {
        if (!frameBuffer.hasRemaining()) {
            val timecode = timecodeBase + timecodeSampleOffset * 1000 / format.sampleRate
            frameBuffer.clear()
            for (postProcessor in postProcessors) {
                postProcessor.process(timecode, frameBuffer)
            }

            frameBuffer.clear()
            timecodeSampleOffset += format.chunkSampleCount.toLong()
        }
    }
}
