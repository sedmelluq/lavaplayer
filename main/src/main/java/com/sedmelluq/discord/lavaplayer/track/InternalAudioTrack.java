package com.sedmelluq.discord.lavaplayer.track;

import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameProvider;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioTrackExecutor;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;

/**
 * Methods of an audio track that should not be visible outside of the library
 */
public interface InternalAudioTrack extends AudioTrack, AudioFrameProvider {
  /**
   * @param executor Executor to assign to the track
   */
  void assignExecutor(AudioTrackExecutor executor);

  /**
   * @return Get the active track executor
   */
  AudioTrackExecutor getActiveExecutor();

  /**
   * Perform any necessary loading and then enter the read/seek loop
   * @param executor The local executor which processes this track
   */
  void process(LocalAudioTrackExecutor executor) throws Exception;
}
