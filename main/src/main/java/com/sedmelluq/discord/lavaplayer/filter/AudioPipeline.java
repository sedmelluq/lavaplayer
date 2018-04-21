package com.sedmelluq.discord.lavaplayer.filter;

import java.nio.ShortBuffer;
import java.util.List;

public class AudioPipeline extends CompositeAudioFilter {
  private final List<AudioFilter> filters;
  private final UniversalPcmAudioFilter first;

  public AudioPipeline(AudioFilterChain chain) {
    this.filters = chain.filters;
    this.first = chain.input;
  }

  @Override
  public void process(float[][] input, int offset, int length) throws InterruptedException {
    first.process(input, offset, length);
  }

  @Override
  public void process(short[] input, int offset, int length) throws InterruptedException {
    first.process(input, offset, length);
  }

  @Override
  public void process(ShortBuffer buffer) throws InterruptedException {
    first.process(buffer);
  }

  @Override
  public void process(short[][] input, int offset, int length) throws InterruptedException {
    first.process(input, offset, length);
  }

  @Override
  protected List<AudioFilter> getFilters() {
    return filters;
  }
}
