package com.sedmelluq.discord.lavaplayer.filter.converter;

import com.sedmelluq.discord.lavaplayer.filter.SplitShortPcmAudioFilter;

import java.nio.ShortBuffer;

/**
 * Filter which takes in PCM data in any representation and outputs it as split short PCM.
 */
public class ToSplitShortAudioFilter extends ConverterAudioFilter {
  private final SplitShortPcmAudioFilter downstream;
  private final int channelCount;
  private final short[][] buffers;

  /**
   * @param downstream The split short PCM filter to pass the output to.
   * @param channelCount Number of channels in the PCM data.
   */
  public ToSplitShortAudioFilter(SplitShortPcmAudioFilter downstream, int channelCount) {
    this.downstream = downstream;
    this.channelCount = channelCount;
    this.buffers = new short[channelCount][];

    for (int i = 0; i < channelCount; i++) {
      this.buffers[i] = new short[BUFFER_SIZE];
    }
  }

  @Override
  public void process(float[][] input, int offset, int length) throws InterruptedException {
    int end = offset + length;

    while (offset < end) {
      int chunkLength = Math.min(end - offset, BUFFER_SIZE);

      for (int channel = 0; channel < channelCount; channel++) {
        for (int chunkPosition = 0; chunkPosition < chunkLength; chunkPosition++) {
          buffers[channel][chunkPosition] = floatToShort(input[channel][offset + chunkPosition]);
        }
      }

      downstream.process(buffers, 0, chunkLength);
    }
  }

  @Override
  public void process(short[] input, int offset, int length) throws InterruptedException {
    int end = offset + length;

    while (end - offset >= channelCount) {
      int chunkLength = Math.min(end - offset, BUFFER_SIZE * channelCount);

      for (int chunkPosition = 0; chunkPosition < chunkLength; chunkPosition++) {
        for (int channel = 0; channel < buffers.length; channel++) {
          buffers[channel][chunkPosition] = floatToShort(input[offset++]);
        }
      }

      downstream.process(buffers, 0, chunkLength);
    }
  }

  @Override
  public void process(ShortBuffer buffer) throws InterruptedException {
    while (buffer.hasRemaining()) {
      int chunkLength = Math.min(buffer.remaining(), BUFFER_SIZE * channelCount);

      for (int chunkPosition = 0; chunkPosition < chunkLength; chunkPosition++) {
        for (int channel = 0; channel < buffers.length; channel++) {
          buffers[channel][chunkPosition] = floatToShort(buffer.get());
        }
      }

      downstream.process(buffers, 0, chunkLength);
    }
  }

  @Override
  public void process(short[][] input, int offset, int length) throws InterruptedException {
    downstream.process(input, offset, length);
  }
}
