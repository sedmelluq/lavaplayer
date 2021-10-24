package com.sedmelluq.discord.lavaplayer.filter;

import com.sedmelluq.discord.lavaplayer.manager.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.natives.samplerate.SampleRateConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * Filter which resamples audio to the specified sample rate
 */
public class ResamplingPcmAudioFilter implements FloatPcmAudioFilter {
    public static final Map<ResamplingQuality, SampleRateConverter.ResamplingType> RESAMPLING_VALUES = new HashMap<>();

    private static final int BUFFER_SIZE = 4096;

    static {
        RESAMPLING_VALUES.put(ResamplingQuality.HIGH, SampleRateConverter.ResamplingType.SINC_MEDIUM_QUALITY);
        RESAMPLING_VALUES.put(ResamplingQuality.MEDIUM, SampleRateConverter.ResamplingType.SINC_FASTEST);
        RESAMPLING_VALUES.put(ResamplingQuality.LOW, SampleRateConverter.ResamplingType.LINEAR);
    }

    private final FloatPcmAudioFilter downstream;
    private final SampleRateConverter[] converters;
    private final SampleRateConverter.Progress progress = new SampleRateConverter.Progress();
    private final float[][] outputSegments;

    /**
     * @param configuration Configuration to use
     * @param channels      Number of channels in input data
     * @param downstream    Next filter in chain
     * @param sourceRate    Source sample rate
     * @param targetRate    Target sample rate
     */
    public ResamplingPcmAudioFilter(AudioConfiguration configuration, int channels, FloatPcmAudioFilter downstream,
                                    int sourceRate, int targetRate) {

        this.downstream = downstream;
        converters = new SampleRateConverter[channels];
        outputSegments = new float[channels][];

        SampleRateConverter.ResamplingType type = getResamplingType(configuration.getResamplingQuality());

        for (int i = 0; i < channels; i++) {
            outputSegments[i] = new float[BUFFER_SIZE];
            converters[i] = new SampleRateConverter(type, 1, sourceRate, targetRate);
        }
    }

    private static SampleRateConverter.ResamplingType getResamplingType(ResamplingQuality quality) {
        return RESAMPLING_VALUES.get(quality);
    }

    @Override
    public void seekPerformed(long requestedTime, long providedTime) {
        for (SampleRateConverter converter : converters) {
            converter.reset();
        }
    }

    @Override
    public void flush() throws InterruptedException {
        // Nothing to do.
    }

    @Override
    public void close() {
        for (SampleRateConverter converter : converters) {
            converter.close();
        }
    }

    @Override
    public void process(float[][] input, int offset, int length) throws InterruptedException {
        do {
            for (int i = 0; i < input.length; i++) {
                converters[i].process(input[i], offset, length, outputSegments[i], 0, BUFFER_SIZE, false, progress);
            }

            offset += progress.getInputUsed();
            length -= progress.getInputUsed();

            if (progress.getOutputGenerated() > 0) {
                downstream.process(outputSegments, 0, progress.getOutputGenerated());
            }
        } while (length > 0 || progress.getOutputGenerated() == BUFFER_SIZE);
    }
}
