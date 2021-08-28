package lavaplayer.filter

/**
 * Audio filter which accepts 16-bit signed PCM samples, with an array per .
 */
interface SplitShortPcmAudioFilter : AudioFilter {
    /**
     * @param input  An array of samples for each channel
     * @param offset Offset in the array
     * @param length Length of the sequence in the array
     * @throws InterruptedException When interrupted externally (or for seek/stop).
     */
    @Throws(InterruptedException::class)
    fun process(input: Array<ShortArray>, offset: Int, length: Int)
}
