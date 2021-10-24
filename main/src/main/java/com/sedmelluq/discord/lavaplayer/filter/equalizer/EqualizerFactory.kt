package com.sedmelluq.discord.lavaplayer.filter.equalizer

import com.sedmelluq.discord.lavaplayer.filter.AudioFilter
import com.sedmelluq.discord.lavaplayer.filter.PcmFilterFactory
import com.sedmelluq.discord.lavaplayer.filter.UniversalPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.track.AudioTrack

/**
 * PCM filter factory which creates a single [Equalizer] filter for every track. Useful in case the equalizer is
 * the only custom filter used.
 */
class EqualizerFactory : EqualizerConfiguration(FloatArray(Equalizer.BAND_COUNT)), PcmFilterFactory {
    override fun buildChain(track: AudioTrack?, format: AudioDataFormat, output: UniversalPcmAudioFilter): List<AudioFilter> {
        return if (Equalizer.isCompatible(format)) {
            listOf<AudioFilter>(Equalizer(format.channelCount, output, bandMultipliers))
        } else {
            emptyList()
        }
    }
}
