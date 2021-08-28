package lavaplayer.container.mp3

import lavaplayer.tools.DataFormatTools.arrayRangeEquals
import lavaplayer.container.mp3.Mp3Seeker
import lavaplayer.natives.mp3.Mp3Decoder
import kotlin.Throws
import java.io.IOException
import lavaplayer.tools.io.SeekableInputStream
import lavaplayer.container.mp3.Mp3ConstantRateSeeker
import lavaplayer.tools.DataFormatTools

/**
 * MP3 seeking support for constant bitrate files or in cases where the variable bitrate format used by the file is not
 * supported. In case the file is not actually CBR, this being used as a fallback may cause inaccurate seeking.
 */
class Mp3ConstantRateSeeker private constructor(
    private val averageFrameSize: Double,
    private val sampleRate: Int,
    private val firstFramePosition: Long,
    private val contentLength: Long
) : Mp3Seeker {
    companion object {
        private const val META_TAG_OFFSET = 36
        private val META_TAGS = arrayOf(
            byteArrayOf('I'.code.toByte(), 'n'.code.toByte(), 'f'.code.toByte(), 'o'.code.toByte()),
            byteArrayOf('L'.code.toByte(), 'A'.code.toByte(), 'M'.code.toByte(), 'E'.code.toByte())
        )

        /**
         * @param firstFramePosition Position of the first frame in the file
         * @param contentLength      Total length of the file
         * @param frameBuffer        Buffer of the first frame
         * @return Constant rate seeker, will always succeed, never null.
         */
        @JvmStatic
        fun createFromFrame(firstFramePosition: Long, contentLength: Long, frameBuffer: ByteArray): Mp3ConstantRateSeeker {
            val sampleRate = Mp3Decoder.getFrameSampleRate(frameBuffer, 0)
            val averageFrameSize = Mp3Decoder.getAverageFrameSize(frameBuffer, 0)
            return Mp3ConstantRateSeeker(averageFrameSize, sampleRate, firstFramePosition, contentLength)
        }

        @JvmStatic
        fun isMetaFrame(frameBuffer: ByteArray): Boolean {
            return META_TAGS.any { arrayRangeEquals(frameBuffer, META_TAG_OFFSET, it) }
        }
    }

    private val maximumFrameCount: Long
        get() = ((contentLength - firstFramePosition + 8) / averageFrameSize).toLong()

    override val duration: Long
        get() = maximumFrameCount * Mp3Decoder.MPEG1_SAMPLES_PER_FRAME * 1000 / sampleRate

    override val isSeekable: Boolean
        get() = true

    @Throws(IOException::class)
    override fun seekAndGetFrameIndex(timecode: Long, inputStream: SeekableInputStream): Long {
        val maximumFrameCount = maximumFrameCount
        val sampleIndex = timecode * sampleRate / 1000
        val frameIndex = (sampleIndex / Mp3Decoder.MPEG1_SAMPLES_PER_FRAME).coerceAtMost(maximumFrameCount)
        val seekPosition = (frameIndex * averageFrameSize).toLong() - 8
        inputStream.seek(firstFramePosition + seekPosition)

        return frameIndex
    }
}
