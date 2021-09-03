package lavaplayer.filter

import java.nio.ShortBuffer

/**
 * Represents an audio pipeline (top-level audio filter chain).
 */
open class AudioPipeline(chain: AudioFilterChain) : CompositeAudioFilter() {
    override val filters: List<AudioFilter> = chain.filters
    private val first: UniversalPcmAudioFilter = chain.input

    @Throws(InterruptedException::class)
    override fun process(input: Array<FloatArray>, offset: Int, length: Int) {
        first.process(input, offset, length)
    }

    @Throws(InterruptedException::class)
    override fun process(input: ShortArray, offset: Int, length: Int) {
        first.process(input, offset, length)
    }

    @Throws(InterruptedException::class)
    override fun process(buffer: ShortBuffer) {
        first.process(buffer)
    }

    @Throws(InterruptedException::class)
    override fun process(input: Array<ShortArray>, offset: Int, length: Int) {
        first.process(input, offset, length)
    }
}
