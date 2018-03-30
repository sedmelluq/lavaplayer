package com.sedmelluq.discord.lavaplayer.player;

import com.sedmelluq.discord.lavaplayer.filter.PcmFilterFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AudioPlayerOptions {
  public final AtomicInteger volumeLevel;
  public final AtomicReference<PcmFilterFactory> filterFactory;

  public AudioPlayerOptions() {
    this.volumeLevel = new AtomicInteger(100);
    this.filterFactory = new AtomicReference<>();
  }
}
