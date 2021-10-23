package lavaplayer.filter

import lavaplayer.track.playback.AudioProcessingContext
import java.nio.ShortBuffer

/**
 * A composite audio filter for filters provided by a [PcmFilterFactory]. Automatically rebuilds the chain
 * whenever the filter factory is changed.
 *
 * @param context    Configuration and output information for processing
 * @param nextFilter The next filter that should be processed after this one.
 */
open class UserProvidedAudioFilters(
    private val context: AudioProcessingContext,
    private val nextFilter: UniversalPcmAudioFilter
) : CompositeAudioFilter() {
    private val hotSwapEnabled: Boolean = context.filterHotSwapEnabled
    private var chain: AudioFilterChain = buildFragment(context, nextFilter)

    override val filters: List<AudioFilter>
        protected get() = chain.filters

    @Throws(InterruptedException::class)
    override fun process(input: Array<FloatArray>, offset: Int, length: Int) {
        checkRebuild()
        chain.input.process(input, offset, length)
    }

    @Throws(InterruptedException::class)
    override fun process(input: ShortArray, offset: Int, length: Int) {
        checkRebuild()
        chain.input.process(input, offset, length)
    }

    @Throws(InterruptedException::class)
    override fun process(buffer: ShortBuffer) {
        checkRebuild()
        chain.input.process(buffer)
    }

    @Throws(InterruptedException::class)
    override fun process(input: Array<ShortArray>, offset: Int, length: Int) {
        checkRebuild()
        chain.input.process(input, offset, length)
    }

    @Throws(InterruptedException::class)
    private fun checkRebuild() {
        if (hotSwapEnabled && context.playerOptions.filterFactory !== chain.context) {
            flush()
            close()
            chain = buildFragment(context, nextFilter)
        }
    }

    companion object {
        private fun buildFragment(
            context: AudioProcessingContext,
            nextFilter: UniversalPcmAudioFilter
        ): AudioFilterChain {
            val factory = context.playerOptions.filterFactory
                ?: return AudioFilterChain(nextFilter, emptyList(), null)

            val filters = factory
                .buildChain(null, context.outputFormat, nextFilter)
                .toMutableList()

            if (filters.isEmpty()) {
                return AudioFilterChain(nextFilter, emptyList(), null)
            }

            filters.reverse()

            val builder = FilterChainBuilder()
            for (filter in filters) {
                builder.addFirst(filter)
            }

            return builder.build(factory, context.outputFormat.channelCount)
        }
    }
}
