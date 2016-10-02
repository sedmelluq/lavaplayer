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
    this.nextChannelIndex = 0;
  }

  @Override
  public void process(short[] input, int offset, int length) throws InterruptedException {
    if (canPassThrough(length)) {
      downstream.process(input, offset, length);
    } else {
      processNormalizer(ShortBuffer.wrap(input, offset, length));
    }
  }

  @Override
  public void process(ShortBuffer buffer) throws InterruptedException {
    if (canPassThrough(buffer.remaining())) {
      downstream.process(buffer);
    } else {
      processNormalizer(buffer);
    }
  }

  private void processNormalizer(ShortBuffer buffer) throws InterruptedException {
    int frameRemaining = inputChannels - nextChannelIndex;
    int commonChannels = Math.min(outputChannels, inputChannels);

    while (buffer.remaining() >= frameRemaining) {
      for (; nextChannelIndex < commonChannels; nextChannelIndex++) {
        lastSample = buffer.get();
        outputBuffer.put(lastSample);
      }

      for (; nextChannelIndex < inputChannels; nextChannelIndex++) {
        buffer.get();
      }

      for (; nextChannelIndex < outputChannels; nextChannelIndex++) {
        outputBuffer.put(lastSample);
      }

      nextChannelIndex = 0;
    }

    outputBuffer.flip();
    downstream.process(outputBuffer);
    outputBuffer.clear();

    int remainingInputChannels = buffer.remaining();

    if (remainingInputChannels > 0) {
      int remainingCommonChannels = Math.min(outputChannels, remainingInputChannels);

      for (; nextChannelIndex < remainingCommonChannels; nextChannelIndex++) {
        lastSample = buffer.get();
        outputBuffer.put(lastSample);
      }

      for (; nextChannelIndex < remainingInputChannels; nextChannelIndex++) {
        buffer.get();
      }
    }
  }

  private boolean canPassThrough(int length) {
    return nextChannelIndex == 0 && inputChannels == outputChannels && (length % inputChannels) == 0;
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
