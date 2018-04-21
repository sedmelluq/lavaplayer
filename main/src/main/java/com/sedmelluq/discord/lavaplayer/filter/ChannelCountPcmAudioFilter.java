package com.sedmelluq.discord.lavaplayer.filter;

import java.nio.ShortBuffer;

/**
 * For short PCM buffers, guarantees that the output has the required number of channels and that no outgoing
 * buffer contains any partial frames.
 *
 * For example if the input is three channels, and output is two channels, then:
 * in [0, 1, 2, 0, 1, 2, 0, 1] out [0, 1, 0, 1] saved [0, 1]
 * in [2, 0, 1, 2] out [0, 1, 0, 1] saved []
 */
public class ChannelCountPcmAudioFilter implements UniversalPcmAudioFilter {
  private final UniversalPcmAudioFilter downstream;
  private final int outputChannels;
  private final ShortBuffer outputBuffer;
  private final int inputChannels;
  private final int commonChannels;
  private final int channelsToAdd;
  private final short[] inputSet;
  private final float[][] splitFloatOutput;
  private final short[][] splitShortOutput;
  private int inputIndex;

  /**
   * @param inputChannels Number of input channels
   * @param outputChannels Number of output channels
   * @param downstream The next filter in line
   */
  public ChannelCountPcmAudioFilter(int inputChannels, int outputChannels, UniversalPcmAudioFilter downstream) {
    this.downstream = downstream;
    this.inputChannels = inputChannels;
    this.outputChannels = outputChannels;
    this.outputBuffer = ShortBuffer.allocate(2048 * inputChannels);
    this.commonChannels = Math.min(outputChannels, inputChannels);
    this.channelsToAdd = outputChannels - commonChannels;
    this.inputSet = new short[inputChannels];
    this.splitFloatOutput = new float[outputChannels][];
    this.splitShortOutput = new short[outputChannels][];
    this.inputIndex = 0;
  }

  @Override
  public void process(short[] input, int offset, int length) throws InterruptedException {
    if (canPassThrough(length)) {
      downstream.process(input, offset, length);
    } else {
      if (inputChannels == 1 && outputChannels == 2) {
        processMonoToStereo(ShortBuffer.wrap(input, offset, length));
      } else {
        processNormalizer(ShortBuffer.wrap(input, offset, length));
      }
    }
  }

  @Override
  public void process(ShortBuffer buffer) throws InterruptedException {
    if (canPassThrough(buffer.remaining())) {
      downstream.process(buffer);
    } else {
      if (inputChannels == 1 && outputChannels == 2) {
        processMonoToStereo(buffer);
      } else {
        processNormalizer(buffer);
      }
    }
  }

  private void processNormalizer(ShortBuffer buffer) throws InterruptedException {
    while (buffer.hasRemaining()) {
      inputSet[inputIndex++] = buffer.get();

      if (inputIndex == inputChannels) {
        outputBuffer.put(inputSet, 0, commonChannels);

        for (int i = 0; i < channelsToAdd; i++) {
          outputBuffer.put(inputSet[0]);
        }

        if (!outputBuffer.hasRemaining()) {
          outputBuffer.flip();
          downstream.process(outputBuffer);
          outputBuffer.clear();
        }

        inputIndex = 0;
      }
    }
  }

  private void processMonoToStereo(ShortBuffer buffer) throws InterruptedException {
    while (buffer.hasRemaining()) {
      short sample = buffer.get();
      outputBuffer.put(sample);
      outputBuffer.put(sample);

      if (!outputBuffer.hasRemaining()) {
        outputBuffer.flip();
        downstream.process(outputBuffer);
        outputBuffer.clear();
      }
    }
  }

  private boolean canPassThrough(int length) {
    return inputIndex == 0 && inputChannels == outputChannels && (length % inputChannels) == 0;
  }

  @Override
  public void process(float[][] input, int offset, int length) throws InterruptedException {
    for (int i = 0; i < commonChannels; i++) {
      splitFloatOutput[i] = input[i];
    }

    for (int i = commonChannels; i < outputChannels; i++) {
      splitFloatOutput[i] = input[0];
    }

    downstream.process(splitFloatOutput, offset, length);
  }

  @Override
  public void process(short[][] input, int offset, int length) throws InterruptedException {
    for (int i = 0; i < commonChannels; i++) {
      splitShortOutput[i] = input[i];
    }

    for (int i = commonChannels; i < outputChannels; i++) {
      splitShortOutput[i] = input[0];
    }

    downstream.process(splitShortOutput, offset, length);
  }

  @Override
  public void seekPerformed(long requestedTime, long providedTime) {
    outputBuffer.clear();
  }

  @Override
  public void flush() throws InterruptedException {
    // Nothing to do.
  }

  @Override
  public void close() {
    // Nothing to do.
  }
}
