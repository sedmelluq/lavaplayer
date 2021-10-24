package com.sedmelluq.discord.lavaplayer.filter

import java.nio.ShortBuffer

/**
 * Audio filter which accepts 16-bit signed PCM samples.
 */
interface ShortPcmAudioFilter : AudioFilter {
    /**
     * @param input  Array of samples
     * @param offset Offset in the array
     * @param length Length of the sequence in the array
     * @throws InterruptedException When interrupted externally (or for seek/stop).
     */
    @Throws(InterruptedException::class)
    fun process(input: ShortArray, offset: Int, length: Int)

    /**
     * @param buffer The buffer of samples
     * @throws InterruptedException When interrupted externally (or for seek/stop).
     */
    @Throws(InterruptedException::class)
    fun process(buffer: ShortBuffer)
}
