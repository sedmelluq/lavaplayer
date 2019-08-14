package com.sedmelluq.discord.lavaplayer.filter.equalizer;

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;

import java.util.Arrays;

/**
 * An equalizer PCM filter. Applies the equalizer with configuration specified by band multipliers (either set
 * externally or using {@link #setGain(int, float)}).
 */
public class Equalizer extends EqualizerConfiguration implements FloatPcmAudioFilter {
  /**
   * Number of bands in the equalizer.
   */
  public static final int BAND_COUNT = 15;

  private static final int SAMPLE_RATE = 48000;

  private final ChannelProcessor[] channels;
  private final FloatPcmAudioFilter next;

  private static final Coefficients[] coefficients48000 = {
      new Coefficients(9.9847546664e-01f, 7.6226668143e-04f, 1.9984647656e+00f),
      new Coefficients(9.9756184654e-01f, 1.2190767289e-03f, 1.9975344645e+00f),
      new Coefficients(9.9616261379e-01f, 1.9186931041e-03f, 1.9960947369e+00f),
      new Coefficients(9.9391578543e-01f, 3.0421072865e-03f, 1.9937449618e+00f),
      new Coefficients(9.9028307215e-01f, 4.8584639242e-03f, 1.9898465702e+00f),
      new Coefficients(9.8485897264e-01f, 7.5705136795e-03f, 1.9837962543e+00f),
      new Coefficients(9.7588512657e-01f, 1.2057436715e-02f, 1.9731772447e+00f),
      new Coefficients(9.6228521814e-01f, 1.8857390928e-02f, 1.9556164694e+00f),
      new Coefficients(9.4080933132e-01f, 2.9595334338e-02f, 1.9242054384e+00f),
      new Coefficients(9.0702059196e-01f, 4.6489704022e-02f, 1.8653476166e+00f),
      new Coefficients(8.5868004289e-01f, 7.0659978553e-02f, 1.7600401337e+00f),
      new Coefficients(7.8409610788e-01f, 1.0795194606e-01f, 1.5450725522e+00f),
      new Coefficients(6.8332861002e-01f, 1.5833569499e-01f, 1.1426447155e+00f),
      new Coefficients(5.5267518228e-01f, 2.2366240886e-01f, 4.0186190803e-01f),
      new Coefficients(4.1811888447e-01f, 2.9094055777e-01f, -7.0905944223e-01f)
  };

  /**
   * @param channelCount Number of channels in the input.
   * @param next The next filter in the chain.
   * @param bandMultipliers The band multiplier values. Keeps using this array internally, so the values can be changed
   *                        externally.
   */
  public Equalizer(int channelCount, FloatPcmAudioFilter next, float[] bandMultipliers) {
    super(bandMultipliers);
    this.channels = createProcessors(channelCount, bandMultipliers);
    this.next = next;
  }

  /**
   * @param channelCount Number of channels in the input.
   * @param next The next filter in the chain.
   */
  public Equalizer(int channelCount, FloatPcmAudioFilter next) {
    this(channelCount, next, new float[BAND_COUNT]);
  }

  /**
   * @param format Audio output format.
   * @return <code>true</code> if the output format is compatible for the equalizer (based on sample rate).
   */
  public static boolean isCompatible(AudioDataFormat format) {
    return format.sampleRate == SAMPLE_RATE;
  }

  @Override
  public void process(float[][] input, int offset, int length) throws InterruptedException {
    for (int channelIndex = 0; channelIndex < channels.length; channelIndex++) {
      channels[channelIndex].process(input[channelIndex], offset, offset + length);
    }

    next.process(input, offset, length);
  }

  @Override
  public void seekPerformed(long requestedTime, long providedTime) {
    for (int channelIndex = 0; channelIndex < channels.length; channelIndex++) {
      channels[channelIndex].reset();
    }
  }

  @Override
  public void flush() throws InterruptedException {
    // Nothing to do here.
  }

  @Override
  public void close() {
    // Nothing to do here.
  }

  private static ChannelProcessor[] createProcessors(int channelCount, float[] bandMultipliers) {
    ChannelProcessor[] processors = new ChannelProcessor[channelCount];

    for (int i = 0; i < channelCount; i++) {
      processors[i] = new ChannelProcessor(bandMultipliers);
    }

    return processors;
  }

  private static class ChannelProcessor {
    private final float[] history;
    private final float[] bandMultipliers;

    private int current;
    private int minusOne;
    private int minusTwo;

    private ChannelProcessor(float[] bandMultipliers) {
      this.history = new float[BAND_COUNT * 6];
      this.bandMultipliers = bandMultipliers;
      this.current = 0;
      this.minusOne = 2;
      this.minusTwo = 1;
    }

    private void process(float[] samples, int startIndex, int endIndex) {
      for (int sampleIndex = startIndex; sampleIndex < endIndex; sampleIndex++) {
        float sample = samples[sampleIndex];
        float result = sample * 0.25f;

        for (int bandIndex = 0; bandIndex < BAND_COUNT; bandIndex++) {
          int x = bandIndex * 6;
          int y = x + 3;

          Coefficients coefficients = coefficients48000[bandIndex];

          float bandResult = coefficients.alpha * (sample - history[x + minusTwo]) +
              coefficients.gamma * history[y + minusOne] -
              coefficients.beta * history[y + minusTwo];

          history[x + current] = sample;
          history[y + current] = bandResult;

          result += bandResult * bandMultipliers[bandIndex];
        }

        samples[sampleIndex] = Math.min(Math.max(result * 4.0f, -1.0f), 1.0f);

        if (++current == 3) {
          current = 0;
        }

        if (++minusOne == 3) {
          minusOne = 0;
        }

        if (++minusTwo == 3) {
          minusTwo = 0;
        }
      }
    }

    private void reset() {
      Arrays.fill(history, 0.0f);
    }
  }

  private static class Coefficients {
    private final float beta;
    private final float alpha;
    private final float gamma;

    private Coefficients(float beta, float alpha, float gamma) {
      this.beta = beta;
      this.alpha = alpha;
      this.gamma = gamma;
    }
  }
}
