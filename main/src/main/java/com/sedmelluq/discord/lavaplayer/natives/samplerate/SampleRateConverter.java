package com.sedmelluq.discord.lavaplayer.natives.samplerate;

import com.sedmelluq.discord.lavaplayer.natives.NativeResourceHolder;

/**
 * Sample rate converter backed by libsamplerate
 */
public class SampleRateConverter extends NativeResourceHolder {
  private final SampleRateLibrary library;
  private final double ratio;
  private final long instance;

  /**
   * @param type Resampling type
   * @param channels Number of channels
   * @param sourceRate Source sample rate
   * @param targetRate Target sample rate
   */
  public SampleRateConverter(ResamplingType type, int channels, int sourceRate, int targetRate) {
    ratio = (double)targetRate / (double)sourceRate;
    library = SampleRateLibrary.getInstance();
    instance = library.create(type.ordinal(), channels);

    if (instance == 0) {
      throw new IllegalStateException("Could not create an instance of sample rate converter.");
    }
  }

  /**
   * Reset the converter, makes sure previous data does not affect next incoming data
   */
  public void reset() {
    checkNotReleased();

    library.reset(instance);
  }

  /**
   * @param input Input buffer
   * @param inputOffset Offset for input buffer
   * @param inputLength Length for input buffer
   * @param output Output buffer
   * @param outputOffset Offset for output buffer
   * @param outputLength Length for output buffer
   * @param endOfInput If this is the last piece of input
   * @param progress Instance that is filled with the progress
   */
  public void process(float[] input, int inputOffset, int inputLength, float[] output, int outputOffset, int outputLength, boolean endOfInput, Progress progress) {
    checkNotReleased();

    int error = library.process(instance, input, inputOffset, inputLength, output, outputOffset, outputLength, endOfInput, ratio, progress.fields);

    if (error != 0) {
      throw new RuntimeException("Failed to convert sample rate, error " + error + ".");
    }
  }

  @Override
  protected void freeResources() {
    library.destroy(instance);
  }

  /**
   * Progress of converting one piece of data
   */
  public static class Progress {
    private final int[] fields = new int[2];

    /**
     * @return Number of samples used from the input buffer
     */
    public int getInputUsed() {
      return fields[0];
    }

    /**
     * @return Number of samples written to the output buffer
     */
    public int getOutputGenerated() {
      return fields[1];
    }
  }

  /**
   * Available resampling types
   */
  public enum ResamplingType {
    SINC_BEST_QUALITY,
    SINC_MEDIUM_QUALITY,
    SINC_FASTEST,
    ZERO_ORDER_HOLD,
    LINEAR
  }
}
