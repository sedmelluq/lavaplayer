package lavaplayer.filter.equalizer

import lavaplayer.filter.PcmFilterFactory
import lavaplayer.track.AudioTrack
import lavaplayer.format.AudioDataFormat
import lavaplayer.filter.UniversalPcmAudioFilter
import lavaplayer.filter.AudioFilter

/**
 * PCM filter factory which creates a single [Equalizer] filter for every track. Useful in case the equalizer is
 * the only custom filter used.
 */
class EqualizerFactory : EqualizerConfiguration(FloatArray(Equalizer.BAND_COUNT)), PcmFilterFactory {
    override fun buildChain(track: AudioTrack?, format: AudioDataFormat?, output: UniversalPcmAudioFilter?): List<AudioFilter> {
        return if (Equalizer.isCompatible(format)) {
            listOf<AudioFilter>(Equalizer(format!!.channelCount, output, bandMultipliers))
        } else {
            emptyList()
        }
    }
}
