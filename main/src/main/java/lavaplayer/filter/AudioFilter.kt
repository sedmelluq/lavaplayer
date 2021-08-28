package lavaplayer.filter

import java.io.Closeable

/**
 * A filter for audio samples
 */
interface AudioFilter : Closeable {
    /**
     * Indicates that the next samples are not a continuation from the previous ones and gives the timecode for the
     * next incoming sample.
     *
     * @param requestedTime Timecode in milliseconds to which the seek was requested to
     * @param providedTime  Timecode in milliseconds to which the seek was actually performed to
     */
    fun seekPerformed(requestedTime: Long, providedTime: Long)

    /**
     * Flush everything to output.
     *
     * @throws InterruptedException When interrupted externally (or for seek/stop).
     */
    @Throws(InterruptedException::class)
    fun flush()

    /**
     * Free all resources. No more input is expected.
     */
    override fun close()
}
