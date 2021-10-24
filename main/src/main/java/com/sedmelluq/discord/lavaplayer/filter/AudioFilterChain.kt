package com.sedmelluq.discord.lavaplayer.filter

/**
 * A chain of audio filters.
 *
 * @param input   See [.input].
 * @param filters See [.filters].
 * @param context See [.context].
 */
data class AudioFilterChain(
    /**
     * The first filter in the stream. Separate field as unlike other filters, this must be an instance of
     * [UniversalPcmAudioFilter] as the input data may be in any representation.
     */
    @JvmField val input: UniversalPcmAudioFilter,
    /**
     * All filters in this chain.
     */
    @JvmField val filters: List<AudioFilter>,
    /**
     * Immutable context/configuration instance that this filter was generated from. May be `null`.
     */
    @JvmField val context: Any?
)
