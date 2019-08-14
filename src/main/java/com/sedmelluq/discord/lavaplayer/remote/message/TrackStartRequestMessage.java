package com.sedmelluq.discord.lavaplayer.remote.message;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

/**
 * The message that is sent to the node when the master requests the node to start playing a track.
 */
public class TrackStartRequestMessage implements RemoteMessage {
  /**
   * The ID for the track executor
   */
  public final long executorId;
  /**
   * Generic track information
   */
  public final AudioTrackInfo trackInfo;
  /**
   * Track specific extra information that is required to initialise the track object
   */
  public final byte[] encodedTrack;
  /**
   * Initial volume of the track
   */
  public final int volume;
  /**
   * Configuration to use for audio processing
   */
  public final AudioConfiguration configuration;
  /**
   * Position to start playing at in milliseconds
   */
  public final long position;

  /**
   * @param executorId The ID for the track executor
   * @param trackInfo Generic track information
   * @param encodedTrack Track specific extra information that is required to initialise the track object
   * @param volume Initial volume of the track
   * @param configuration Configuration to use for audio processing
   * @param position Position to start playing at in milliseconds
   */
  public TrackStartRequestMessage(long executorId, AudioTrackInfo trackInfo, byte[] encodedTrack, int volume,
                                  AudioConfiguration configuration, long position) {

    this.executorId = executorId;
    this.encodedTrack = encodedTrack;
    this.trackInfo = trackInfo;
    this.volume = volume;
    this.configuration = configuration;
    this.position = position;
  }
}
