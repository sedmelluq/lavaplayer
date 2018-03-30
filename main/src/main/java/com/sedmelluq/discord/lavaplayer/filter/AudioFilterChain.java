package com.sedmelluq.discord.lavaplayer.filter;

import java.util.List;

public class AudioFilterChain {
  public final UniversalPcmAudioFilter input;
  public final List<AudioFilter> filters;
  public final Object context;

  public AudioFilterChain(UniversalPcmAudioFilter input, List<AudioFilter> filters, Object context) {
    this.input = input;
    this.filters = filters;
    this.context = context;
  }
}
