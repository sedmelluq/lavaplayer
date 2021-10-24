package com.sedmelluq.discord.lavaplayer.filter.equalizer

/**
 * Holder of equalizer configuration.
 *
 * @param bandMultipliers The band multiplier values. Keeps using this array internally, so the values can be changed
 * externally.
 */
open class EqualizerConfiguration(protected val bandMultipliers: FloatArray) {
    /**
     * @param band  The index of the band. If this is not a valid band index, the method has no effect.
     * @param value The multiplier for this band. Default value is 0. Valid values are from -0.25 to 1. -0.25 means that
     * the given frequency is completely muted and 0.25 means it is doubled. Note that this may change the
     * volume of the output.
     */
    fun setGain(band: Int, value: Float) {
        if (isValidBand(band)) {
            bandMultipliers[band] = value.coerceAtMost(1.0f).coerceAtLeast(-0.25f)
        }
    }

    /**
     * @param band The index of the band.
     * @return The multiplier for this band. Default value is 0.
     */
    fun getGain(band: Int): Float {
        return if (isValidBand(band)) {
            bandMultipliers[band]
        } else {
            0.0f
        }
    }

    private fun isValidBand(band: Int): Boolean {
        return band >= 0 && band < bandMultipliers.size
    }
}
