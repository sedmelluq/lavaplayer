package com.sedmelluq.discord.lavaplayer.filter.converter;

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;

import java.nio.ShortBuffer;

/**
 * Filter which takes in PCM data in any representation and outputs it as float PCM.
 */
public class ToFloatAudioFilter extends ConverterAudioFilter {
  private final FloatPcmAudioFilter downstream;
  private final int channelCount;
  private final float[][] buffers;

  /**
   * @param downstream The float PCM filter to pass the output to.
   * @param channelCount Number of channels in the PCM data.
   */
  public ToFloatAudioFilter(FloatPcmAudioFilter downstream, int channelCount) {
    this.downstream = downstream;
    this.channelCount = channelCount;
    this.buffers = new float[channelCount][];

    for (int i = 0; i < channelCount; i++) {
      this.buffers[i] = new float[BUFFER_SIZE];
    }
  }

  @Override
  public void process(float[][] input, int offset, int length) throws InterruptedException {
    downstream.process(input, offset, length);
  }

  @Override
  public void process(short[] input, int offset, int length) throws InterruptedException {
    int end = offset + length;

    while (end - offset >= channelCount) {
      int chunkLength = Math.min((end - offset) / channelCount, BUFFER_SIZE);

      for (int chunkPosition = 0; chunkPosition < chunkLength; chunkPosition++) {
        for (int channel = 0; channel < channelCount; channel++) {
          buffers[channel][chunkPosition] = shortToFloat(input[offset++]);
        }
      }

      downstream.process(buffers, 0, chunkLength);
    }
  }

  @Override
  public void process(ShortBuffer buffer) throws InterruptedException {
    while (buffer.hasRemaining()) {
      int chunkLength = Math.min(buffer.remaining() / channelCount, BUFFER_SIZE);

      if (chunkLength == 0) {
        break;
      }

      for (int chunkPosition = 0; chunkPosition < chunkLength; chunkPosition++) {
        for (int channel = 0; channel < buffers.length; channel++) {
          buffers[channel][chunkPosition] = shortToFloat(buffer.get());
        }
      }

      downstream.process(buffers, 0, chunkLength);
    }
  }

  @Override
  public void process(short[][] input, int offset, int length) throws InterruptedException {
    int end = offset + length;

    while (offset < end) {
      int chunkLength = Math.min(end - offset, BUFFER_SIZE);

      for (int channel = 0; channel < buffers.length; channel++) {
        for (int chunkPosition = 0; chunkPosition < chunkLength; chunkPosition++) {
          buffers[channel][chunkPosition] = shortToFloat(input[channel][offset + chunkPosition]);
        }
      }

      offset += chunkLength;
      downstream.process(buffers, 0, chunkLength);
    }
  }

  private static float shortToFloat(short value) {
    return value / 32768.0f;
  }
}
