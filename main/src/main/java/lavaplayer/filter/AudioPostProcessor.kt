package lavaplayer.filter

import java.io.Closeable
import kotlin.Throws
import java.lang.InterruptedException
import java.nio.ShortBuffer

/**
 * Audio chunk post processor.
 */
interface AudioPostProcessor : Closeable {
    /**
     * Receives chunk buffer in its final PCM format with the sample count, sample rate and channel count matching that of
     * the output format.
     *
     * @param timecode Absolute starting timecode of the chunk in milliseconds
     * @param buffer   PCM buffer of samples in the chunk
     * @throws InterruptedException When interrupted externally (or for seek/stop).
     */
    @Throws(InterruptedException::class)
    fun process(timecode: Long, buffer: ShortBuffer?)

    /**
     * Frees up all resources this processor is holding internally.
     */
    override fun close()
}
