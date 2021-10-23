package lavaplayer.filter.volume

import java.nio.ShortBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

/**
 * Class used to apply a volume level to short PCM buffers
 *
 * @param initialVolume Initial volume level (only useful for getLastVolume() as specified with each call)
 */
class PcmVolumeProcessor(initialVolume: Int) {
    /**
     * Last volume level used with this processor
     */
    var lastVolume = -1

    private var integerMultiplier = 0

    init {
        setupMultipliers(initialVolume)
    }

    /**
     * @param initialVolume The input volume of the samples
     * @param targetVolume  The target volume of the samples
     * @param buffer        The buffer containing the samples
     */
    fun applyVolume(initialVolume: Int, targetVolume: Int, buffer: ShortBuffer) {
        if (initialVolume in 1..99) {
            setupMultipliers(initialVolume)
            unapplyCurrentVolume(buffer)
        }

        setupMultipliers(targetVolume)
        applyCurrentVolume(buffer)
    }

    private fun setupMultipliers(activeVolume: Int) {
        if (lastVolume != activeVolume) {
            lastVolume = activeVolume
            integerMultiplier = if (activeVolume <= 150) {
                val floatMultiplier = tan((activeVolume * 0.0079f).toDouble()).toFloat()
                (floatMultiplier * 10000).toInt()
            } else {
                24621 * activeVolume / 150
            }
        }
    }

    private fun applyCurrentVolume(buffer: ShortBuffer) {
        if (lastVolume == 100) {
            return
        }

        val endOffset = buffer.limit()
        for (i in buffer.position() until endOffset) {
            val value = buffer[i] * integerMultiplier / 10000
            buffer.put(i, max(-32767, min(32767, value)).toShort())
        }
    }

    private fun unapplyCurrentVolume(buffer: ShortBuffer) {
        if (integerMultiplier == 0) {
            return
        }

        val endOffset = buffer.limit()
        for (i in buffer.position() until endOffset) {
            val value = buffer[i] * 10000 / integerMultiplier
            buffer.put(i, max(-32767, min(32767, value)).toShort())
        }
    }
}
