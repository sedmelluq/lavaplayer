package lavaplayer.filter.equalizer;

import lavaplayer.filter.AudioFilter;
import lavaplayer.filter.PcmFilterFactory;
import lavaplayer.filter.UniversalPcmAudioFilter;
import lavaplayer.format.AudioDataFormat;
import lavaplayer.track.AudioTrack;

import java.util.Collections;
import java.util.List;

/**
 * PCM filter factory which creates a single {@link Equalizer} filter for every track. Useful in case the equalizer is
 * the only custom filter used.
 */
public class EqualizerFactory extends EqualizerConfiguration implements PcmFilterFactory {
    /**
     * Creates a new instance no gains applied initially.
     */
    public EqualizerFactory() {
        super(new float[Equalizer.BAND_COUNT]);
    }

    @Override
    public List<AudioFilter> buildChain(AudioTrack track, AudioDataFormat format, UniversalPcmAudioFilter output) {
        if (Equalizer.isCompatible(format)) {
            return Collections.singletonList(new Equalizer(format.channelCount, output, bandMultipliers));
        } else {
            return Collections.emptyList();
        }
    }
}
