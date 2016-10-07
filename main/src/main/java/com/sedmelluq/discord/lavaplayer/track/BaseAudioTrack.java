package com.sedmelluq.discord.lavaplayer.track;

import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioTrackExecutor;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract base for all audio tracks with an executor
 */
public abstract class BaseAudioTrack implements InternalAudioTrack {
  protected final AudioTrackExecutor executor;
  protected final AudioTrackInfo trackInfo;
  protected final AtomicLong accurateDuration;

  /**
   * @param executor Track executor
   * @param trackInfo Track info
   */
  public BaseAudioTrack(AudioTrackExecutor executor, AudioTrackInfo trackInfo) {
    this.executor = executor;
    this.trackInfo = trackInfo;
    this.accurateDuration = new AtomicLong();
    executor.assign(this);
  }

  @Override
  public AudioTrackExecutor getExecutor() {
    return executor;
  }

  @Override
  public void stop() {
    executor.stop();
  }

  @Override
  public AudioTrackState getState() {
    return executor.getState();
  }

  @Override
  public String getIdentifier() {
    return executor.getIdentifier();
  }

  @Override
  public long getPosition() {
    return executor.getPosition();
  }

  @Override
  public void setPosition(long position) {
    executor.setPosition(position);
  }

  @Override
  public void setLoop(AudioLoop loop) {
    executor.setLoop(loop);
  }

  @Override
  public AudioFrame provide() {
    return executor.provide();
  }

  @Override
  public AudioTrackInfo getInfo() {
    return trackInfo;
  }

  @Override
  public long getDuration() {
    long accurate = accurateDuration.get();

    if (accurate == 0) {
      return trackInfo.length * 1000L;
    } else {
      return accurate;
    }
  }

  @Override
  public AudioTrack makeClone() {
    throw new UnsupportedOperationException();
  }
}
