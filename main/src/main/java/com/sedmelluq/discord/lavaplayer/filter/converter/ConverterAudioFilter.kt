package com.sedmelluq.discord.lavaplayer.filter.converter

import com.sedmelluq.discord.lavaplayer.filter.UniversalPcmAudioFilter

/**
 * Base class for converter filters which have no internal state.
 */
abstract class ConverterAudioFilter : UniversalPcmAudioFilter {
    override fun seekPerformed(requestedTime: Long, providedTime: Long) {
        // Nothing to do.
    }

    @Throws(InterruptedException::class)
    override fun flush() {
        // Nothing to do.
    }

    override fun close() {
        // Nothing to do.
    }

    companion object {
        protected const val BUFFER_SIZE = 4096

        @JvmStatic
        protected fun floatToShort(value: Float): Short {
            return (value * 32768.0f).toInt().toShort()
        }
    }
}
