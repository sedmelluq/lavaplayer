package com.sedmelluq.discord.lavaplayer.track.playback;

import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Encapsulates common behavior shared by different audio frame providers.
 */
public class AudioFrameProviderTools {
  /**
   * @param provider Delegates a call to frame provide without timeout to the timed version of it.
   * @return The audio frame from provide method.
   */
  public static AudioFrame delegateToTimedProvide(AudioFrameProvider provider) {
    try {
      return provider.provide(0, TimeUnit.MILLISECONDS);
    } catch (TimeoutException | InterruptedException e) {
      ExceptionTools.keepInterrupted(e);
      throw new RuntimeException(e);
    }
  }
}
