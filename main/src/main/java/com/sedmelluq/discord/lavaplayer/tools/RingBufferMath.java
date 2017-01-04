package com.sedmelluq.discord.lavaplayer.tools;

import java.util.function.Function;

/**
 * Utility class for calculating averages on the last N values, with input and output transformers.
 */
public class RingBufferMath {
  private final double[] values;
  private final Function<Double, Double> inputProcessor;
  private final Function<Double, Double> outputProcessor;
  private double sum;
  private int position;
  private int size;

  /**
   * @param size Maximum number of values to remember.
   * @param inputProcessor Input transformer.
   * @param outputProcessor Output transformer.
   */
  public RingBufferMath(int size, Function<Double, Double> inputProcessor, Function<Double, Double> outputProcessor) {
    this.values = new double[size];
    this.inputProcessor = inputProcessor;
    this.outputProcessor = outputProcessor;
  }

  /**
   * @param value Original value to add (before transformation)
   */
  public void add(double value) {
    value = inputProcessor.apply(value);

    sum -= values[position];
    values[position] = value;
    sum += values[position];

    position = (position + 1) == values.length ? 0 : position + 1;
    size = Math.min(values.length, size + 1);
  }

  /**
   * @return Transformed mean of the internal values.
   */
  public double mean() {
    if (size == 0) {
      return outputProcessor.apply(0.0);
    } else {
      return outputProcessor.apply(sum / size);
    }
  }
}
