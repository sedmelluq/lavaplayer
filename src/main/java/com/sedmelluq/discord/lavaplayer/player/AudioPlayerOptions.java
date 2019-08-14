package com.sedmelluq.discord.lavaplayer.player;

import com.sedmelluq.discord.lavaplayer.filter.PcmFilterFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mutable options of an audio player which may be applied in real-time.
 */
public class AudioPlayerOptions {
  /**
   * Volume level of the audio, see {@link AudioPlayer#setVolume(int)}. Applied in real-time.
   */
  public final AtomicInteger volumeLevel;
  /**
   * Current PCM filter factory. Applied in real-time.
   */
  public final AtomicReference<PcmFilterFactory> filterFactory;
  /**
   * Current frame buffer size. If not set, the global default is used. Changing this only affects the next track that
   * is started.
   */
  public final AtomicReference<Integer> frameBufferDuration;

  /**
   * New instance of player options. By default, frame buffer duration is not set, hence taken from global settings.
   */
  public AudioPlayerOptions() {
    this.volumeLevel = new AtomicInteger(100);
    this.filterFactory = new AtomicReference<>();
    this.frameBufferDuration = new AtomicReference<>();
  }
}
