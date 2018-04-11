package com.sedmelluq.discord.lavaplayer.filter;

import com.sedmelluq.discord.lavaplayer.filter.converter.ToFloatAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.converter.ToShortAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.converter.ToSplitShortAudioFilter;

import java.util.*;

/**
 * Builds audio filter chains based on the input format.
 */
public class FilterChainBuilder {
  private final int channelCount;
  private final List<AudioFilter> filters = new ArrayList<>();

  public FilterChainBuilder(int channelCount) {
    this.channelCount = channelCount;
  }

  public void addFirst(AudioFilter filter) {
    filters.add(filter);
  }

  public AudioFilter first() {
    return filters.get(filters.size() - 1);
  }

  public FloatPcmAudioFilter makeFirstFloat() {
    AudioFilter first = first();

    if (first instanceof FloatPcmAudioFilter) {
      return (FloatPcmAudioFilter) first;
    } else {
      return prependUniversalFilter(first, channelCount);
    }
  }

  public UniversalPcmAudioFilter makeFirstUniversal(int channelCountOverride) {
    AudioFilter first = first();

    if (first instanceof UniversalPcmAudioFilter) {
      return (UniversalPcmAudioFilter) first;
    } else {
      return prependUniversalFilter(first, channelCountOverride);
    }
  }

  public UniversalPcmAudioFilter makeFirstUniversal() {
    return makeFirstUniversal(channelCount);
  }

  private UniversalPcmAudioFilter prependUniversalFilter(AudioFilter first, int channelCountOverride) {
    UniversalPcmAudioFilter universalInput;

    if (first instanceof SplitShortPcmAudioFilter) {
      universalInput = new ToSplitShortAudioFilter((SplitShortPcmAudioFilter) first, channelCountOverride);
    } else if (first instanceof FloatPcmAudioFilter) {
      universalInput = new ToFloatAudioFilter((FloatPcmAudioFilter) first, channelCountOverride);
    } else if (first instanceof ShortPcmAudioFilter) {
      universalInput = new ToShortAudioFilter((ShortPcmAudioFilter) first, channelCountOverride);
    } else {
      throw new RuntimeException("Filter must implement at least one data type.");
    }

    addFirst(universalInput);
    return universalInput;
  }

  public AudioFilterChain build(Object context, int channelCountOverride) {
    UniversalPcmAudioFilter firstFilter = makeFirstUniversal(channelCountOverride);
    return new AudioFilterChain(firstFilter, filters, context);
  }

  public AudioFilterChain build(Object context) {
    return build(context, channelCount);
  }
}
