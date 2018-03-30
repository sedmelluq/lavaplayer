package com.sedmelluq.discord.lavaplayer.filter;

import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserProvidedAudioFilters extends CompositeAudioFilter {
  private final AudioProcessingContext context;
  private final UniversalPcmAudioFilter nextFilter;
  private final boolean hotSwapEnabled;
  private AudioFilterChain chain;

  public UserProvidedAudioFilters(AudioProcessingContext context, UniversalPcmAudioFilter nextFilter) {
    this.context = context;
    this.nextFilter = nextFilter;
    this.hotSwapEnabled = context.filterHotSwapEnabled;
    this.chain = buildFragment(context, nextFilter);
  }

  private static AudioFilterChain buildFragment(AudioProcessingContext context,
                                                UniversalPcmAudioFilter nextFilter) {

    PcmFilterFactory factory = context.playerOptions.filterFactory.get();

    if (factory == null) {
      return new AudioFilterChain(nextFilter, Collections.emptyList(), null);
    } else {
      PcmFormat outputFormat = new PcmFormat(context.outputFormat.channelCount, context.outputFormat.sampleRate);
      FilterChainBuilder builder = new FilterChainBuilder(outputFormat);

      List<AudioFilter> filters = new ArrayList<>(factory.buildChain(null, context.outputFormat, nextFilter));
      Collections.reverse(filters);

      for (AudioFilter filter : filters) {
        builder.addFirst(filter);
      }

      return builder.build(factory);
    }
  }

  @Override
  protected List<AudioFilter> getFilters() {
    return chain.filters;
  }

  @Override
  public void process(float[][] input, int offset, int length) throws InterruptedException {
    checkRebuild();
    chain.input.process(input, offset, length);
  }

  @Override
  public void process(short[] input, int offset, int length) throws InterruptedException {
    checkRebuild();
    chain.input.process(input, offset, length);
  }

  @Override
  public void process(ShortBuffer buffer) throws InterruptedException {
    checkRebuild();
    chain.input.process(buffer);
  }

  @Override
  public void process(short[][] input, int offset, int length) throws InterruptedException {
    checkRebuild();
    chain.input.process(input, offset, length);
  }

  private void checkRebuild() throws InterruptedException {
    if (hotSwapEnabled) {
      if (context.playerOptions.filterFactory.get() != chain.context) {
        flush();
        close();
        chain = buildFragment(context, nextFilter);
      }
    }
  }
}
