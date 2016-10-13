package com.sedmelluq.discord.lavaplayer.player;

import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;
import com.sedmelluq.discord.lavaplayer.player.event.PlayerPauseEvent;
import com.sedmelluq.discord.lavaplayer.player.event.PlayerResumeEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackExceptionEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackStuckEvent;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackStateListener;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameProvider;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An audio player that is capable of playing audio tracks and provides audio frames from the currently playing track.
 */
public class AudioPlayer implements AudioFrameProvider, TrackStateListener {
  private static final Logger log = LoggerFactory.getLogger(AudioPlayer.class);

  private final AtomicReference<InternalAudioTrack> activeTrack;
  private volatile long lastReceiveTime;
  private volatile boolean stuckEventSent;
  private volatile InternalAudioTrack shadowTrack;
  private final AtomicBoolean paused;
  private final AudioPlayerManager manager;
  private final List<AudioEventListener> listeners;
  private final AtomicInteger volumeLevel;

  /**
   * @param manager Audio player manager which this player is attached to
   */
  public AudioPlayer(AudioPlayerManager manager) {
    activeTrack = new AtomicReference<>();
    paused = new AtomicBoolean();
    this.manager = manager;
    listeners = new ArrayList<>();
    volumeLevel = new AtomicInteger(100);
  }

  /**
   * @return Currently playing track
   */
  public AudioTrack getPlayingTrack() {
    return activeTrack.get();
  }

  /**
   * @param track The track to start playing
   */
  public void playTrack(AudioTrack track) {
    startTrack(track, false);
  }

  /**
   * @param track The track to start playing
   * @param noInterrupt Whether to only start if nothing else is playing
   * @return True if the track was started
   */
  public boolean startTrack(AudioTrack track, boolean noInterrupt) {
    InternalAudioTrack newTrack = (InternalAudioTrack) track;
    InternalAudioTrack previousTrack = null;

    if (noInterrupt) {
      if (!activeTrack.compareAndSet(null, newTrack)) {
        return false;
      }
    } else {
      previousTrack = activeTrack.getAndSet(newTrack);
    }

    lastReceiveTime = System.nanoTime();
    stuckEventSent = false;

    if (previousTrack != null) {
      previousTrack.stop();
      dispatchEvent(new TrackEndEvent(this, previousTrack, true));

      shadowTrack = previousTrack;
    }

    manager.executeTrack(this, newTrack, manager.getConfiguration(), volumeLevel);
    return true;
  }

  /**
   * Stop currently playing track.
   */
  public void stopTrack() {
    shadowTrack = null;

    InternalAudioTrack previousTrack = activeTrack.getAndSet(null);
    if (previousTrack != null) {
      previousTrack.stop();
      dispatchEvent(new TrackEndEvent(this, previousTrack, true));
    }
  }

  private AudioFrame provideShadowFrame() {
    InternalAudioTrack shadow = shadowTrack;
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

  @Override
  public AudioFrame provide() {
    InternalAudioTrack track;

    if (paused.get()) {
      return null;
    }

    while ((track = activeTrack.get()) != null) {
      AudioFrame frame = track.provide();

      if (frame != null) {
        lastReceiveTime = System.nanoTime();
        shadowTrack = null;

        if (frame.isTerminator()) {
          handleTerminator(track);
          continue;
        }
      } else {
        if (!stuckEventSent && System.nanoTime() - lastReceiveTime > manager.getTrackStuckThresholdNanos()) {
          stuckEventSent = true;
          dispatchEvent(new TrackStuckEvent(this, track, TimeUnit.NANOSECONDS.toMillis(manager.getTrackStuckThresholdNanos())));
        }

        frame = provideShadowFrame();
      }

      return frame;
    }

    return null;
  }

  private void handleTerminator(InternalAudioTrack track) {
    if (activeTrack.compareAndSet(track, null)) {
      dispatchEvent(new TrackEndEvent(this, track, false));
    }
  }

  public int getVolume() {
    return volumeLevel.get();
  }

  public void setVolume(int volume) {
    volumeLevel.set(Math.min(150, Math.max(0, volume)));
  }

  /**
   * @return Whether the player is paused
   */
  public boolean isPaused() {
    return paused.get();
  }

  /**
   * @param value True to pause, false to resume
   */
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
   * Destroy the player and stop all playing tracks.
   */
  public void destroy() {
    stopTrack();
  }

  /**
   * Add a listener to events from this player.
   * @param listener New listener
   */
  public void addListener(AudioEventListener listener) {
    synchronized (listeners) {
      listeners.add(listener);
    }
  }

  /**
   * Clear the list of attached event listeners.
   */
  public void clearListeners() {
    synchronized (listeners) {
      listeners.clear();
    }
  }

  private void dispatchEvent(AudioEvent event) {
    log.debug("Firing an event with class {}", event.getClass().getSimpleName());

    synchronized (listeners) {
      for (AudioEventListener listener : listeners) {
        listener.onEvent(event);
      }
    }
  }

  @Override
  public void onTrackException(AudioTrack track, FriendlyException exception) {
    dispatchEvent(new TrackExceptionEvent(this, track, exception));
  }
}
