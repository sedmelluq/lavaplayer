package com.sedmelluq.discord.lavaplayer.filter;

import com.sedmelluq.discord.lavaplayer.filter.converter.ToFloatAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.converter.ToShortAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.converter.ToSplitShortAudioFilter;

import java.util.*;

/**
 * Builds audio filter chains based on the input format.
 */
public class FilterChainBuilder {
  private final PcmFormat format;
  private final List<AudioFilter> filters = new ArrayList<>();

  public FilterChainBuilder(PcmFormat format) {
    this.format = format;
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
      return prependUniversalFilter(first);
    }
  }

  public UniversalPcmAudioFilter makeFirstUniversal() {
    AudioFilter first = first();

    if (first instanceof UniversalPcmAudioFilter) {
      return (UniversalPcmAudioFilter) first;
    } else {
      return prependUniversalFilter(first);
    }
  }

  private UniversalPcmAudioFilter prependUniversalFilter(AudioFilter first) {
    UniversalPcmAudioFilter universalInput;

    if (first instanceof SplitShortPcmAudioFilter) {
      universalInput = new ToSplitShortAudioFilter((SplitShortPcmAudioFilter) first, format.channelCount);
    } else if (first instanceof FloatPcmAudioFilter) {
      universalInput = new ToFloatAudioFilter((FloatPcmAudioFilter) first, format.channelCount);
    } else if (first instanceof ShortPcmAudioFilter) {
      universalInput = new ToShortAudioFilter((ShortPcmAudioFilter) first, format.channelCount);
    } else {
      throw new RuntimeException("Filter must implement at least one data type.");
    }

    addFirst(universalInput);
    return universalInput;
  }

  public AudioFilterChain build(Object context) {
    UniversalPcmAudioFilter firstFilter = makeFirstUniversal();
    return new AudioFilterChain(firstFilter, filters, context);
  }

}
