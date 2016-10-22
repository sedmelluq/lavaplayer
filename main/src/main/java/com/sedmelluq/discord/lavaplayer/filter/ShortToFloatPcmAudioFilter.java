package com.sedmelluq.discord.lavaplayer.filter;

import java.nio.ShortBuffer;

/**
 * Converts signed 16-bit PCM samples to floating point PCM samples
 */
public class ShortToFloatPcmAudioFilter implements ShortPcmAudioFilter, SplitShortPcmAudioFilter {
  private static final int BUFFER_SIZE = 4096;

  private final FloatPcmAudioFilter downstream;
  private final float[][] buffers;

  /**
   * @param channelCount Number of channels in input data
   * @param downstream Next filter in chain
   */
  public ShortToFloatPcmAudioFilter(int channelCount, FloatPcmAudioFilter downstream) {
    this.downstream = downstream;
    this.buffers = new float[channelCount][];

    for (int i = 0; i < channelCount; i++) {
      this.buffers[i] = new float[BUFFER_SIZE];
    }
  }

  @Override
  public void seekPerformed(long requestedTime, long providedTime) {
    downstream.seekPerformed(requestedTime, providedTime);
  }

  @Override
  public void flush() throws InterruptedException {
    downstream.flush();
  }

  @Override
  public void close() {
    downstream.close();
  }

  @Override
  public void process(short[] input, int offset, int length) throws InterruptedException {
    process(ShortBuffer.wrap(input, offset, length));
  }

  @Override
  public void process(ShortBuffer buffer) throws InterruptedException {
    int pos = 0;

    while (buffer.remaining() > 0) {
      for (int i = 0; i < buffers.length; i++) {
        buffers[i][pos] = buffer.get() / 32768.0f;
      }

      if (++pos == BUFFER_SIZE) {
        downstream.process(buffers, 0, pos);
        pos = 0;
      }
    }

    if (pos > 0) {
      downstream.process(buffers, 0, pos);
    }
  }

  @Override
  public void process(short[][] input, int offset, int length) throws InterruptedException {
    int inputPosition = offset;
    int inputEndPosition = offset + length;

    while (inputPosition < inputEndPosition) {
      int chunkSize = Math.min(buffers[0].length, inputEndPosition - inputPosition);

      for (int channel = 0; channel < buffers.length; channel++) {
        for (int i = 0; i < chunkSize; i++) {
          buffers[channel][i] = input[channel][inputPosition + i] / 32768.0f;
        }
      }

      downstream.process(buffers, 0, chunkSize);
      inputPosition += chunkSize;
    }
  }
}
