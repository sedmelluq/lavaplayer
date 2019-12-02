package com.sedmelluq.lavaplayer.core.player.filter;

import com.sedmelluq.lavaplayer.core.format.AudioDataFormat;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackContext;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An composite audio filter for filters provided by a {@link PcmFilterFactory}. Automatically rebuilds the chain
 * whenever the filter factory is changed.
 */
public class UserProvidedAudioFilters extends CompositeAudioFilter {
  private final AudioPlaybackContext context;
  private final UniversalPcmAudioFilter nextFilter;
  private final boolean hotSwapEnabled;
  private AudioFilterChain chain;

  /**
   * @param context Configuration and output information for processing
   * @param nextFilter The next filter that should be processed after this one.
   */
  public UserProvidedAudioFilters(AudioPlaybackContext context, UniversalPcmAudioFilter nextFilter) {
    this.context = context;
    this.nextFilter = nextFilter;
    this.hotSwapEnabled = context.getConfiguration().isFilterHotSwapEnabled();
    this.chain = buildFragment(context, nextFilter);
  }

  private static AudioFilterChain buildFragment(AudioPlaybackContext context, UniversalPcmAudioFilter nextFilter) {
    AudioDataFormat format = context.getConfiguration().getOutputFormat();
    PcmFilterFactory factory = context.getConfiguration().getFilterFactory();

    if (factory == null) {
      return new AudioFilterChain(nextFilter, Collections.emptyList(), null);
    } else {
      FilterChainBuilder builder = new FilterChainBuilder();

      List<AudioFilter> filters = new ArrayList<>(
          factory.buildChain(null, format, nextFilter)
      );

      if (filters.isEmpty()) {
        return new AudioFilterChain(nextFilter, Collections.emptyList(), null);
      }

      Collections.reverse(filters);

      for (AudioFilter filter : filters) {
        builder.addFirst(filter);
      }

      return builder.build(factory, format.channelCount);
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
    if (hotSwapEnabled && context.getConfiguration().getFilterFactory() != chain.context) {
      flush();
      close();
      chain = buildFragment(context, nextFilter);
    }
  }
}
