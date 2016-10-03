package com.sedmelluq.discord.lavaplayer.track;

import com.sedmelluq.discord.lavaplayer.track.playback.AudioTrackExecutor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Audio track which delegates its processing to another track. The delegate does not have to be known when the
 * track is created, but is passed when processDelegate() is called.
 */
public abstract class DelegatedAudioTrack extends BaseAudioTrack {
  private InternalAudioTrack delegate;

  /**
   * @param executor Track executor
   * @param trackInfo Track info
   */
  public DelegatedAudioTrack(AudioTrackExecutor executor, AudioTrackInfo trackInfo) {
    super(executor, trackInfo);
  }

  protected synchronized void processDelegate(InternalAudioTrack delegate, AtomicInteger volumeLevel) throws Exception {
    if (this.delegate != null) {
      throw new IllegalStateException("Cannot assign delegate twice.");
    }

    this.delegate = delegate;

    delegate.process(volumeLevel);
  }

  @Override
  public long getDuration() {
    if (delegate != null) {
      return delegate.getDuration();
    } else {
      synchronized (this) {
        if (delegate != null) {
          return delegate.getDuration();
        } else {
          return super.getDuration();
        }
      }
    }
  }
}
