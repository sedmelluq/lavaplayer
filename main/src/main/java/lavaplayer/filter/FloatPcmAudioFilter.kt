package lavaplayer.filter

import lavaplayer.filter.AudioFilter
import kotlin.Throws
import java.lang.InterruptedException

/**
 * Audio filter which accepts floating point PCM samples.
 */
interface FloatPcmAudioFilter : AudioFilter {
    /**
     * @param input  An array of samples for each channel
     * @param offset Offset in the arrays to start at
     * @param length Length of the target sequence in arrays
     * @throws InterruptedException When interrupted externally (or for seek/stop).
     */
    @Throws(InterruptedException::class)
    fun process(input: Array<FloatArray?>, offset: Int, length: Int)
}
