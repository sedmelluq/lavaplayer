package com.sedmelluq.discord.lavaplayer.track;

import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameProvider;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioTrackExecutor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Methods of an audio track that should not be visible outside of the library
 */
public interface InternalAudioTrack extends AudioTrack, AudioFrameProvider {
  /**
   * @return Get the associated executor
   */
  AudioTrackExecutor getExecutor();

  /**
   * Perform any necessary loading and then enter the read/seek loop
   * @param volumeLevel Mutable volume level to use when processing the track
   */
  void process(AtomicInteger volumeLevel) throws Exception;
}
