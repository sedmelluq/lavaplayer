package com.sedmelluq.discord.lavaplayer.track;

import java.util.concurrent.atomic.AtomicReference;

import static com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler.MarkerState.BYPASSED;
import static com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler.MarkerState.LATE;
import static com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler.MarkerState.OVERWRITTEN;
import static com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler.MarkerState.REACHED;
import static com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler.MarkerState.REMOVED;

/**
 * Tracks the state of a track position marker.
 */
public class TrackMarkerTracker {
  private final AtomicReference<TrackMarker> current = new AtomicReference<>();

  /**
   * Set a new track position marker.
   * @param marker Marker
   * @param currentTimecode Current timecode of the track when this marker is set
   */
  public void set(TrackMarker marker, long currentTimecode) {
    TrackMarker previous = current.getAndSet(marker);

    if (previous != null) {
      previous.handler.handle(marker != null ? OVERWRITTEN : REMOVED);
    }

    if (marker != null && currentTimecode >= marker.timecode) {
      trigger(marker, LATE);
    }
  }

  /**
   * Remove the current marker.
   * @return The removed marker.
   */
  public TrackMarker remove() {
    return current.getAndSet(null);
  }

  /**
   * Trigger and remove the marker with the specified state.
   * @param state The state of the marker to pass to the handler.
   */
  public void trigger(TrackMarkerHandler.MarkerState state) {
    TrackMarker marker = current.getAndSet(null);

    if (marker != null) {
      marker.handler.handle(state);
    }
  }

  /**
   * Check a timecode which was reached by normal playback, trigger REACHED if necessary.
   * @param timecode Timecode which was reached by normal playback.
   */
  public void checkPlaybackTimecode(long timecode) {
    TrackMarker marker = current.get();

    if (marker != null && timecode >= marker.timecode) {
      trigger(marker, REACHED);
    }
  }

  /**
   * Check a timecode which was reached by seeking, trigger BYPASSED if necessary.
   * @param timecode Timecode which was reached by seeking.
   */
  public void checkSeekTimecode(long timecode) {
    TrackMarker marker = current.get();

    if (marker != null && timecode >= marker.timecode) {
      trigger(marker, BYPASSED);
    }
  }

  private void trigger(TrackMarker marker, TrackMarkerHandler.MarkerState state) {
    if (current.compareAndSet(marker, null)) {
      marker.handler.handle(state);
    }
  }
}
