package com.sedmelluq.discord.lavaplayer.track.playback;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import com.sedmelluq.discord.lavaplayer.track.TrackMarkerTracker;
import com.sedmelluq.discord.lavaplayer.track.TrackStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Executor implementation which is used before a track has actually been executed. Saves the position and loop
 * information, which is applied to the actual executor when one is attached.
 */
public class PrimordialAudioTrackExecutor implements AudioTrackExecutor {
  private static final Logger log = LoggerFactory.getLogger(LocalAudioTrackExecutor.class);

  private final AudioTrackInfo trackInfo;
  private final TrackMarkerTracker markerTracker;

  private volatile long position;

  /**
   * @param trackInfo Information of the track this executor is used with
   */
  public PrimordialAudioTrackExecutor(AudioTrackInfo trackInfo) {
    this.trackInfo = trackInfo;
    this.markerTracker = new TrackMarkerTracker();
  }

  @Override
  public AudioFrameBuffer getAudioBuffer() {
    return null;
  }

  @Override
  public void execute(TrackStateListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void stop() {
    log.info("Tried to stop track {} which is not playing.", trackInfo.identifier);
  }

  @Override
  public long getPosition() {
    return position;
  }

  @Override
  public void setPosition(long timecode) {
    position = timecode;
    markerTracker.checkSeekTimecode(timecode);
  }

  @Override
  public AudioTrackState getState() {
    return AudioTrackState.INACTIVE;
  }

  @Override
  public void setMarker(TrackMarker marker) {
    markerTracker.set(marker, position);
  }

  @Override
  public boolean failedBeforeLoad() {
    return false;
  }

  @Override
  public AudioFrame provide() {
    return provide(0, TimeUnit.MILLISECONDS);
  }

  @Override
  public AudioFrame provide(long timeout, TimeUnit unit) {
    return null;
  }

  @Override
  public boolean provide(MutableAudioFrame targetFrame) {
    return false;
  }

  @Override
  public boolean provide(MutableAudioFrame targetFrame, long timeout, TimeUnit unit)
      throws TimeoutException, InterruptedException {

    return false;
  }

  /**
   * Apply the position and loop state that had been set on this executor to an actual executor.
   * @param executor The executor to apply the state to
   */
  public void applyStateToExecutor(AudioTrackExecutor executor) {
    if (position != 0) {
      executor.setPosition(position);
    }

    executor.setMarker(markerTracker.remove());
  }
}
