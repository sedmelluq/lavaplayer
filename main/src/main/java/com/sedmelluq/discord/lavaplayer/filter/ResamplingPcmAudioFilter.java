package com.sedmelluq.discord.lavaplayer.filter;

import com.sedmelluq.discord.lavaplayer.natives.samplerate.SampleRateConverter;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

/**
 * Filter which resamples audio to the specified sample rate
 */
public class ResamplingPcmAudioFilter implements FloatPcmAudioFilter {
  private static final int BUFFER_SIZE = 4096;

  private final FloatPcmAudioFilter downstream;
  private final SampleRateConverter[] converters;
  private final SampleRateConverter.Progress progress = new SampleRateConverter.Progress();
  private final float[][] outputSegments;

  /**
   * @param manager Audio player manager which is used for configuration
   * @param channels Number of channels in input data
   * @param downstream Next filter in chain
   * @param sourceRate Source sample rate
   * @param targetRate Target sample rate
   */
  public ResamplingPcmAudioFilter(AudioPlayerManager manager, int channels, FloatPcmAudioFilter downstream, int sourceRate, int targetRate) {
    this.downstream = downstream;
    converters = new SampleRateConverter[channels];
    outputSegments = new float[channels][];

    SampleRateConverter.ResamplingType type = getResamplingType(manager.getResamplingQuality());

    for (int i = 0; i < channels; i++) {
      outputSegments[i] = new float[BUFFER_SIZE];
      converters[i] = new SampleRateConverter(type, 1, sourceRate, targetRate);
    }
  }

  @Override
  public void seekPerformed(long requestedTime, long providedTime) {
    for (SampleRateConverter converter : converters) {
      converter.reset();
    }

    downstream.seekPerformed(requestedTime, providedTime);
  }

  @Override
  public void flush() throws InterruptedException {
    downstream.flush();
  }

  @Override
  public void close() {
    for (SampleRateConverter converter : converters) {
      converter.close();
    }

    downstream.close();
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

  private static SampleRateConverter.ResamplingType getResamplingType(AudioPlayerManager.ResamplingQuality quality) {
    switch (quality) {
      case HIGH:
        return SampleRateConverter.ResamplingType.SINC_MEDIUM_QUALITY;
      case MEDIUM:
        return SampleRateConverter.ResamplingType.SINC_FASTEST;
      case LOW:
      default:
        return SampleRateConverter.ResamplingType.LINEAR;
    }
  }
}
