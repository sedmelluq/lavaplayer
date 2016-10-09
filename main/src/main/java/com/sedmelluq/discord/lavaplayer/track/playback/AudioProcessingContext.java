package com.sedmelluq.discord.lavaplayer.track.playback;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Context for processing audio. Contains configuration for encoding and the output where the frames go to.
 */
public class AudioProcessingContext {
  /**
   * Audio encoding or filtering related configuration
   */
  public final AudioConfiguration configuration;
  /**
   * Consumer for the produced audio frames
   */
  public final AudioFrameConsumer frameConsumer;
  /**
   * Mutable volume level for the audio
   */
  public final AtomicInteger volumeLevel;

  /**
   * @param configuration Audio encoding or filtering related configuration
   * @param frameConsumer Consumer for the produced audio frames
   * @param volumeLevel Mutable volume level for the audio
   */
  public AudioProcessingContext(AudioConfiguration configuration, AudioFrameConsumer frameConsumer, AtomicInteger volumeLevel) {
    this.configuration = configuration;
    this.frameConsumer = frameConsumer;
    this.volumeLevel = volumeLevel;
  }
}
