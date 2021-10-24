package com.sedmelluq.discord.lavaplayer.filter

/**
 * A PCM filter which must be able to correctly process all representations of PCM input data, as specified by the
 * methods of the interfaces it extends.
 */
interface UniversalPcmAudioFilter : ShortPcmAudioFilter, SplitShortPcmAudioFilter, FloatPcmAudioFilter
