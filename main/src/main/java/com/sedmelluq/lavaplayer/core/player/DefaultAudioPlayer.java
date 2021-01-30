package com.sedmelluq.lavaplayer.core.player;

import com.sedmelluq.lavaplayer.core.tools.collections.CopyOnUpdateIdentityList;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrackEndReason;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrackFactory;
import com.sedmelluq.lavaplayer.core.player.configuration.AudioConfiguration;
import com.sedmelluq.lavaplayer.core.player.configuration.DefaultOverlayAudioConfiguration;
import com.sedmelluq.lavaplayer.core.player.configuration.OverlayAudioConfiguration;
import com.sedmelluq.lavaplayer.core.player.event.AudioPlayerEvent;
import com.sedmelluq.lavaplayer.core.player.event.AudioPlayerEventListener;
import com.sedmelluq.lavaplayer.core.player.event.PlayerPauseEvent;
import com.sedmelluq.lavaplayer.core.player.event.PlayerResumeEvent;
import com.sedmelluq.lavaplayer.core.player.event.TrackEndEvent;
import com.sedmelluq.lavaplayer.core.player.event.TrackExceptionEvent;
import com.sedmelluq.lavaplayer.core.player.event.TrackStartEvent;
import com.sedmelluq.lavaplayer.core.player.event.TrackStuckEvent;
import com.sedmelluq.lavaplayer.core.player.frame.AudioFrame;
import com.sedmelluq.lavaplayer.core.player.frame.AudioFrameProviderTools;
import com.sedmelluq.lavaplayer.core.player.frame.MutableAudioFrame;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrack;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrackRequest;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrackStateListener;
import com.sedmelluq.lavaplayer.core.player.track.ExecutableAudioTrack;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.player.track.AudioTrackEndReason.CLEANUP;
import static com.sedmelluq.lavaplayer.core.player.track.AudioTrackEndReason.FINISHED;
import static com.sedmelluq.lavaplayer.core.player.track.AudioTrackEndReason.LOAD_FAILED;
import static com.sedmelluq.lavaplayer.core.player.track.AudioTrackEndReason.REPLACED;
import static com.sedmelluq.lavaplayer.core.player.track.AudioTrackEndReason.STOPPED;

/**
 * An audio player that is capable of playing audio tracks and provides audio frames from the currently playing track.
 */
public class DefaultAudioPlayer implements AudioPlayer, AudioTrackStateListener {
  private static final Logger log = LoggerFactory.getLogger(AudioPlayer.class);

  private final AudioTrackFactory trackFactory;
  private final OverlayAudioConfiguration configuration;
  private volatile ExecutableAudioTrack activeTrack;
  private volatile long lastRequestTime;
  private volatile long lastReceiveTime;
  private volatile boolean stuckEventSent;
  private volatile ExecutableAudioTrack shadowTrack;
  private final AtomicBoolean paused;
  private final CopyOnUpdateIdentityList<AudioPlayerEventListener> listeners;
  private final Object trackSwitchLock;

  public DefaultAudioPlayer(AudioTrackFactory trackFactory, AudioConfiguration configuration) {
    this.trackFactory = trackFactory;
    this.configuration = new DefaultOverlayAudioConfiguration(configuration);
    activeTrack = null;
    paused = new AtomicBoolean();
    listeners = new CopyOnUpdateIdentityList<>();
    trackSwitchLock = new Object();
  }

  /**
   * @return Currently playing track
   */
  public AudioTrack getPlayingTrack() {
    return activeTrack;
  }

  @Override
  public AudioTrack playTrack(AudioTrackRequest request) {
    ExecutableAudioTrack newTrack;
    ExecutableAudioTrack previousTrack;

    synchronized (trackSwitchLock) {
      previousTrack = activeTrack;

      if (!request.getReplaceExisting() && previousTrack != null) {
        return null;
      }

      newTrack = trackFactory.create(request, configuration);

      activeTrack = newTrack;
      lastRequestTime = System.currentTimeMillis();
      lastReceiveTime = System.nanoTime();
      stuckEventSent = false;

      if (previousTrack != null) {
        previousTrack.stop();
        dispatchEvent(new TrackEndEvent(this, previousTrack, newTrack == null ? STOPPED : REPLACED));

        shadowTrack = previousTrack;
      }
    }

    if (newTrack == null) {
      shadowTrack = null;
      return null;
    }

    dispatchEvent(new TrackStartEvent(this, newTrack));
    newTrack.execute(this);
    return newTrack;
  }

  /**
   * Stop currently playing track.
   */
  @Override
  public void stopTrack() {
    stopWithReason(STOPPED);
  }

  private void stopWithReason(AudioTrackEndReason reason) {
    shadowTrack = null;

    synchronized (trackSwitchLock) {
      ExecutableAudioTrack previousTrack = activeTrack;
      activeTrack = null;

      if (previousTrack != null) {
        previousTrack.stop();
        dispatchEvent(new TrackEndEvent(this, previousTrack, reason));
      }
    }
  }

  private AudioFrame provideShadowFrame() {
    ExecutableAudioTrack shadow = shadowTrack;
    AudioFrame frame = null;

    if (shadow != null) {
      frame = shadow.provide();

      if (frame != null && frame.isTerminator()) {
        shadowTrack = null;
        frame = null;
      }
    }

    return frame;
  }

  private boolean provideShadowFrame(MutableAudioFrame targetFrame) {
    ExecutableAudioTrack shadow = shadowTrack;

    if (shadow != null && shadow.provide(targetFrame)) {
      if (targetFrame.isTerminator()) {
        shadowTrack = null;
        return false;
      }

      return true;
    }

    return false;
  }

  @Override
  public AudioFrame provide() {
    return AudioFrameProviderTools.delegateToTimedProvide(this);
  }

  @Override
  public AudioFrame provide(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
    ExecutableAudioTrack track;

    lastRequestTime = System.currentTimeMillis();

    if (timeout == 0 && paused.get()) {
      return null;
    }

    while ((track = activeTrack) != null) {
      AudioFrame frame = timeout > 0 ? track.provide(timeout, unit) : track.provide();

      if (frame != null) {
        lastReceiveTime = System.nanoTime();
        shadowTrack = null;

        if (frame.isTerminator()) {
          handleTerminator(track);
          continue;
        }
      } else if (timeout == 0) {
        checkStuck(track);

        frame = provideShadowFrame();
      }

      return frame;
    }

    return null;
  }

  @Override
  public boolean provide(MutableAudioFrame targetFrame) {
    try {
      return provide(targetFrame, 0, TimeUnit.MILLISECONDS);
    } catch (TimeoutException | InterruptedException e) {
      ExceptionTools.keepInterrupted(e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean provide(MutableAudioFrame targetFrame, long timeout, TimeUnit unit)
      throws TimeoutException, InterruptedException {

    ExecutableAudioTrack track;

    lastRequestTime = System.currentTimeMillis();

    if (timeout == 0 && paused.get()) {
      return false;
    }

    while ((track = activeTrack) != null) {
      if (timeout > 0 ? track.provide(targetFrame, timeout, unit) : track.provide(targetFrame)) {
        lastReceiveTime = System.nanoTime();
        shadowTrack = null;

        if (targetFrame.isTerminator()) {
          handleTerminator(track);
          continue;
        }

        return true;
      } else if (timeout == 0) {
        checkStuck(track);
        return provideShadowFrame(targetFrame);
      } else {
        return false;
      }
    }

    return false;
  }

  @Override
  public OverlayAudioConfiguration getConfiguration() {
    return configuration;
  }

  /**
   * @return Whether the player is paused
   */
  @Override
  public boolean isPaused() {
    return paused.get();
  }

  /**
   * @param value True to pause, false to resume
   */
  @Override
  public void setPaused(boolean value) {
    if (paused.compareAndSet(!value, value)) {
      if (value) {
        dispatchEvent(new PlayerPauseEvent(this));
      } else {
        dispatchEvent(new PlayerResumeEvent(this));
        lastReceiveTime = System.nanoTime();
      }
    }
  }

  /**
   * Add a listener to events from this player.
   * @param listener New listener
   */
  @Override
  public void addListener(AudioPlayerEventListener listener) {
    synchronized (trackSwitchLock) {
      listeners.add(listener);
    }
  }

  /**
   * Remove an attached listener using identity comparison.
   * @param listener The listener to remove
   */
  @Override
  public void removeListener(AudioPlayerEventListener listener) {
    synchronized (trackSwitchLock) {
      listeners.remove(listener);
    }
  }

  @Override
  public void checkCleanup() {
    AudioTrack track = getPlayingTrack();

    if (track != null && System.currentTimeMillis() - lastRequestTime >= configuration.getTrackCleanupThreshold()) {
      log.debug("Triggering cleanup on an audio player playing track {}", track);

      stopWithReason(CLEANUP);
    }
  }

  @Override
  public void close() {
    stopTrack();
  }

  private void dispatchEvent(AudioPlayerEvent event) {
    log.debug("Firing an event with class {}", event.getClass().getSimpleName());

    synchronized (trackSwitchLock) {
      for (AudioPlayerEventListener listener : listeners.items) {
        try {
          listener.onEvent(event);
        } catch (Exception e) {
          log.error("Handler of event {} threw an exception.", event, e);
        }
      }
    }
  }

  @Override
  public void onTrackException(AudioTrack track, FriendlyException exception) {
    dispatchEvent(new TrackExceptionEvent(this, track, exception));
  }

  @Override
  public void onTrackStuck(AudioTrack track, long thresholdMs) {
    dispatchEvent(new TrackStuckEvent(this, track, thresholdMs));
  }

  private void handleTerminator(ExecutableAudioTrack track) {
    synchronized (trackSwitchLock) {
      if (activeTrack == track) {
        activeTrack = null;

        dispatchEvent(new TrackEndEvent(this, track, track.failedBeforeLoad() ? LOAD_FAILED : FINISHED));
      }
    }
  }

  private void checkStuck(AudioTrack track) {
    long threshold = configuration.getTrackStuckThreshold();
    long thresholdNanos = TimeUnit.MILLISECONDS.toNanos(threshold);

    if (!stuckEventSent && System.nanoTime() - lastReceiveTime > thresholdNanos) {
      stuckEventSent = true;
      dispatchEvent(new TrackStuckEvent(this, track, threshold));
    }
  }
}
