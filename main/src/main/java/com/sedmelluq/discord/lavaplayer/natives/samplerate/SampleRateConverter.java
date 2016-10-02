package com.sedmelluq.discord.lavaplayer.natives.samplerate;

import com.sedmelluq.discord.lavaplayer.natives.NativeResourceHolder;

import static com.sedmelluq.discord.lavaplayer.natives.samplerate.SampleRateLibrary.Type.SINC_MEDIUM_QUALITY;

/**
 * Sample rate converter backed by libsamplerate
 */
public class SampleRateConverter extends NativeResourceHolder {
  private final SampleRateLibrary library;
  private final double ratio;
  private final long instance;

  /**
   * @param channels Number of channels
   * @param sourceRate Source sample rate
   * @param targetRate Target sample rate
   */
  public SampleRateConverter(int channels, int sourceRate, int targetRate) {
    ratio = (double)targetRate / (double)sourceRate;
    library = SampleRateLibrary.getInstance();
    instance = library.create(SINC_MEDIUM_QUALITY.ordinal(), channels);

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
}
