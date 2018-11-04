package com.sedmelluq.discord.lavaplayer.filter.converter;

import com.sedmelluq.discord.lavaplayer.filter.ShortPcmAudioFilter;

import java.nio.ShortBuffer;

/**
 * Filter which takes in PCM data in any representation and outputs it as short PCM.
 */
public class ToShortAudioFilter extends ConverterAudioFilter {
  private final ShortPcmAudioFilter downstream;
  private final int channelCount;
  private final short[] outputBuffer;

  /**
   * @param downstream The short PCM filter to pass the output to.
   * @param channelCount Number of channels in the PCM data.
   */
  public ToShortAudioFilter(ShortPcmAudioFilter downstream, int channelCount) {
    this.downstream = downstream;
    this.channelCount = channelCount;
    this.outputBuffer = new short[BUFFER_SIZE * channelCount];
  }

  @Override
  public void process(float[][] input, int offset, int length) throws InterruptedException {
    int end = offset + length;

    while (offset < end) {
      int chunkSize = Math.min(BUFFER_SIZE, end - offset);
      int writePosition = 0;

      for (int chunkPosition = 0; chunkPosition < chunkSize; chunkPosition++) {
        for (int channel = 0; channel < channelCount; channel++) {
          outputBuffer[writePosition++] = floatToShort(input[channel][chunkPosition]);
        }
      }

      offset += chunkSize;
      downstream.process(outputBuffer, 0, chunkSize);
    }
  }

  @Override
  public void process(short[] input, int offset, int length) throws InterruptedException {
    downstream.process(input, offset, length);
  }

  @Override
  public void process(ShortBuffer buffer) throws InterruptedException {
    downstream.process(buffer);
  }

  @Override
  public void process(short[][] input, int offset, int length) throws InterruptedException {
    int end = offset + length;

    while (offset < end) {
      int chunkSize = Math.min(BUFFER_SIZE, end - offset);
      int writePosition = 0;

      for (int chunkPosition = 0; chunkPosition < chunkSize; chunkPosition++) {
        for (int channel = 0; channel < channelCount; channel++) {
          outputBuffer[writePosition++] = input[channel][chunkPosition];
        }
      }

      offset += chunkSize;
      downstream.process(outputBuffer, 0, chunkSize);
    }
  }
}
