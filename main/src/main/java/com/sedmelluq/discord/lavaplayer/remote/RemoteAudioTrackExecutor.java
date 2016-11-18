package com.sedmelluq.discord.lavaplayer.remote;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import com.sedmelluq.discord.lavaplayer.track.TrackMarkerTracker;
import com.sedmelluq.discord.lavaplayer.track.TrackStateListener;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameBuffer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler.MarkerState.ENDED;
import static com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler.MarkerState.STOPPED;

/**
 * This executor delegates the actual audio processing to a remote node.
 */
public class RemoteAudioTrackExecutor implements AudioTrackExecutor {
  private static final Logger log = LoggerFactory.getLogger(RemoteAudioTrackExecutor.class);

  private static final long NO_SEEK = -1;
  private static final int BUFFER_DURATION_MS = 2500;

  private final AudioTrack track;
  private final AudioConfiguration configuration;
  private final RemoteNodeManager remoteNodeManager;
  private final AtomicInteger volumeLevel;
  private final long executorId;
  private final AudioFrameBuffer frameBuffer = new AudioFrameBuffer(BUFFER_DURATION_MS);
  private final AtomicLong lastFrameTimecode = new AtomicLong();
  private final AtomicLong pendingSeek = new AtomicLong(NO_SEEK);
  private final TrackMarkerTracker markerTracker = new TrackMarkerTracker();
  private volatile TrackStateListener activeListener;
  private volatile boolean hasReceivedData;
  private volatile boolean hasStarted;
  private volatile Throwable trackException;

  /**
   * @param track Audio track to play
   * @param configuration Configuration for audio processing
   * @param remoteNodeManager Manager of remote nodes
   * @param volumeLevel Mutable volume level
   */
  public RemoteAudioTrackExecutor(AudioTrack track, AudioConfiguration configuration, RemoteNodeManager remoteNodeManager, AtomicInteger volumeLevel) {
    this.track = track;
    this.configuration = configuration;
    this.remoteNodeManager = remoteNodeManager;
    this.volumeLevel = volumeLevel;
    this.executorId = System.nanoTime();
  }

  /**
   * @return The unique ID for this executor
   */
  public long getExecutorId() {
    return executorId;
  }

  /**
   *
   * @return The configuration to use for processing audio
   */
  public AudioConfiguration getConfiguration() {
    return configuration;
  }

  /**
   * @return The current volume of the track
   */
  public int getVolume() {
    return volumeLevel.get();
  }

  /**
   * @return The track that this executor is playing
   */
  public AudioTrack getTrack() {
    return track;
  }

  /**
   * @return The position of a seek that has not completed. Value is -1 in case no seeking is in progress.
   */
  public long getPendingSeek() {
    return pendingSeek.get();
  }

  /**
   * Clear the current seeking if its position matches the specified position
   * @param position The position to compare with
   */
  public void clearSeek(long position) {
    if (position != NO_SEEK) {
      frameBuffer.setClearOnInsert();

      if (pendingSeek.compareAndSet(position, NO_SEEK)) {
        markerTracker.checkSeekTimecode(position);
      }
    }
  }

  /**
   * Send the specified exception as an event to the active state listener.
   * @param exception Exception to send
   */
  public void dispatchException(FriendlyException exception) {
    TrackStateListener currentListener = activeListener;

    ExceptionTools.log(log, exception, track.getIdentifier());

    if (currentListener != null) {
      trackException = exception;
      currentListener.onTrackException(track, exception);
    }
  }

  /**
   * Mark that this track has received data from the node.
   */
  public void receivedData() {
    hasReceivedData = true;
  }

  /**
   * Detach the currently active listener, so no useless reference would be kept and no events would be sent there.
   */
  public void detach() {
    activeListener = null;

    markerTracker.trigger(ENDED);
  }

  @Override
  public AudioFrameBuffer getAudioBuffer() {
    return frameBuffer;
  }

  @Override
  public void execute(TrackStateListener listener) {
    try {
      hasStarted = true;
      activeListener = listener;
      remoteNodeManager.startPlaying(this);
    } catch (Throwable throwable) {
      listener.onTrackException(track, ExceptionTools.wrapUnfriendlyExceptions(
          "An error occurred when trying to start track remotely.", FriendlyException.Severity.FAULT, throwable));

      ExceptionTools.rethrowErrors(throwable);
    }
  }

  @Override
  public void stop() {
    frameBuffer.lockBuffer();
    frameBuffer.setTerminateOnEmpty();
    frameBuffer.clear();

    markerTracker.trigger(STOPPED);

    remoteNodeManager.onTrackEnd(null, track, AudioTrackEndReason.STOPPED);
  }

  @Override
  public long getPosition() {
    return lastFrameTimecode.get();
  }

  @Override
  public void setPosition(long timecode) {
    pendingSeek.set(timecode);
  }

  @Override
  public AudioTrackState getState() {
    if (hasStarted && activeListener == null) {
      return AudioTrackState.FINISHED;
    } else if (!hasReceivedData) {
      return AudioTrackState.LOADING;
    } else {
      return AudioTrackState.PLAYING;
    }
  }

  @Override
  public void setMarker(TrackMarker marker) {
    markerTracker.set(marker, getPosition());
  }

  @Override
  public AudioFrame provide() {
    AudioFrame frame = frameBuffer.provide();

    if (frame != null && !frame.isTerminator()) {
      lastFrameTimecode.set(frame.timecode);

      if (pendingSeek.get() == NO_SEEK && !frameBuffer.hasClearOnInsert()) {
        markerTracker.checkPlaybackTimecode(frame.timecode);
      }
    }

    return frame;
  }

  @Override
  public boolean failedBeforeLoad() {
    return trackException != null && !hasReceivedData;
  }
}
