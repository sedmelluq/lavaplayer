package com.sedmelluq.discord.lavaplayer.track;

import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameProvider;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioTrackExecutor;

/**
 * Methods of an audio track that should not be visible outside of the library
 */
public interface InternalAudioTrack extends AudioTrack, AudioFrameProvider {
  /**
   * @return Get the associated executor
   */
  AudioTrackExecutor getExecutor();

  /**
   * @return The identifier of the track
   */
  String getIdentifier();

  /**
   * Perform any necessary loading and then enter the read/seek loop
   */
  void process() throws Exception;
}
