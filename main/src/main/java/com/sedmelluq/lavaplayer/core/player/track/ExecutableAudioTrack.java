package com.sedmelluq.lavaplayer.core.player.track;

import com.sedmelluq.lavaplayer.core.player.frame.AudioFrameProvider;

public interface ExecutableAudioTrack extends AudioTrack, AudioFrameProvider {
  /**
   * Execute the track, which means that this thread will fill the frame buffer until the track finishes or is stopped.
   * @param listener Listener for track state events
   */
  void execute(AudioTrackStateListener listener);

  void stop();

  /**
   * @return True if this track threw an exception before it provided any audio.
   */
  boolean failedBeforeLoad();
}
