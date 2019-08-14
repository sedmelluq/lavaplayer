package com.sedmelluq.discord.lavaplayer.filter;

import java.util.List;

/**
 * A chain of audio filters.
 */
public class AudioFilterChain {
  /**
   * The first filter in the stream. Separate field as unlike other filters, this must be an instance of
   * {@link UniversalPcmAudioFilter} as the input data may be in any representation.
   */
  public final UniversalPcmAudioFilter input;

  /**
   * All filters in this chain.
   */
  public final List<AudioFilter> filters;

  /**
   * Immutable context/configuration instance that this filter was generated from. May be <code>null</code>.
   */
  public final Object context;

  /**
   * @param input See {@link #input}.
   * @param filters See {@link #filters}.
   * @param context See {@link #context}.
   */
  public AudioFilterChain(UniversalPcmAudioFilter input, List<AudioFilter> filters, Object context) {
    this.input = input;
    this.filters = filters;
    this.context = context;
  }
}
