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
public class ChannelCountPcmAudioFilter implements ShortPcmAudioFilter {
  private final ShortPcmAudioFilter downstream;
  private final int outputChannels;
  private final ShortBuffer outputBuffer;
  private final int inputChannels;
  private final ShortBuffer inputSet;
  private int nextChannelIndex;
  private short lastSample;

  /**
   * @param inputChannels Number of input channels
   * @param outputChannels Number of output channels
   * @param downstream The next filter in line
   */
  public ChannelCountPcmAudioFilter(int inputChannels, int outputChannels, ShortPcmAudioFilter downstream) {
    this.downstream = downstream;
    this.inputChannels = inputChannels;
    this.outputChannels = outputChannels;
    this.outputBuffer = ShortBuffer.allocate(2048 * inputChannels);
    this.inputSet = ShortBuffer.allocate(inputChannels);
    this.nextChannelIndex = 0;
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
    int commonChannels = Math.min(outputChannels, inputChannels);
    int channelsToAdd = outputChannels - commonChannels;

    while (buffer.hasRemaining()) {
      inputSet.put(buffer.get());

      if (!inputSet.hasRemaining()) {
        for (int i = 0; i < commonChannels; i++) {
          outputBuffer.put(inputSet.get());
        }

        for (int i = 0; i < channelsToAdd; i++) {
          outputBuffer.put(inputSet.get(0));
        }

        if (!outputBuffer.hasRemaining()) {
          outputBuffer.flip();
          downstream.process(outputBuffer);
          outputBuffer.clear();
        }

        inputSet.position(0);
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
    return inputSet.position() == 0 && inputChannels == outputChannels && (length % inputChannels) == 0;
  }

  @Override
  public void seekPerformed(long requestedTime, long providedTime) {
    downstream.seekPerformed(requestedTime, providedTime);

    outputBuffer.clear();
    nextChannelIndex = 0;
  }

  @Override
  public void flush() throws InterruptedException {
    downstream.flush();
  }

  @Override
  public void close() {
    downstream.close();
  }
}
