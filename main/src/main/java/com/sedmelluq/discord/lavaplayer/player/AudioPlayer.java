package com.sedmelluq.discord.lavaplayer.player;

import com.sedmelluq.discord.lavaplayer.filter.PcmFilterFactory;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;
import com.sedmelluq.discord.lavaplayer.player.event.PlayerPauseEvent;
import com.sedmelluq.discord.lavaplayer.player.event.PlayerResumeEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackExceptionEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackStartEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackStuckEvent;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackStateListener;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameProvider;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameProviderTools;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason.CLEANUP;
import static com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason.FINISHED;
import static com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason.LOAD_FAILED;
import static com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason.REPLACED;
import static com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason.STOPPED;

/**
 * An audio player that is capable of playing audio tracks and provides audio frames from the currently playing track.
 */
public interface AudioPlayer extends AudioFrameProvider {
  /**
   * @return Currently playing track
   */
  AudioTrack getPlayingTrack();

  /**
   * @param track The track to start playing
   */
  void playTrack(AudioTrack track);

  /**
   * @param track The track to start playing, passing null will stop the current track and return false
   * @param noInterrupt Whether to only start if nothing else is playing
   * @return True if the track was started
   */
  boolean startTrack(AudioTrack track, boolean noInterrupt);

  /**
   * Stop currently playing track.
   */
  void stopTrack();

  int getVolume();

  void setVolume(int volume);

  void setFilterFactory(PcmFilterFactory factory);

  void setFrameBufferDuration(Integer duration);

  /**
   * @return Whether the player is paused
   */
  boolean isPaused();

  /**
   * @param value True to pause, false to resume
   */
  void setPaused(boolean value);

  /**
   * Destroy the player and stop playing track.
   */
  void destroy();

  /**
   * Add a listener to events from this player.
   * @param listener New listener
   */
  void addListener(AudioEventListener listener);

  /**
   * Remove an attached listener using identity comparison.
   * @param listener The listener to remove
   */
  void removeListener(AudioEventListener listener);

  /**
   * Check if the player should be "cleaned up" - stopped due to nothing using it, with the given threshold.
   * @param threshold Threshold in milliseconds to use
   */
  void checkCleanup(long threshold);
}
