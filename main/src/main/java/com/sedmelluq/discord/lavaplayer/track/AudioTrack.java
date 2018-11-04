package com.sedmelluq.discord.lavaplayer.track;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;

/**
 * A playable audio track
 */
public interface AudioTrack extends AudioItem {
  /**
   * @return Track meta information
   */
  AudioTrackInfo getInfo();

  /**
   * @return The identifier of the track
   */
  String getIdentifier();

  /**
   * @return The current execution state of the track
   */
  AudioTrackState getState();

  /**
   * Stop the track if it is currently playing
   */
  void stop();

  /**
   * @return True if the track is seekable.
   */
  boolean isSeekable();

  /**
   * @return Get the current position of the track in milliseconds
   */
  long getPosition();

  /**
   * Seek to the specified position.
   *
   * @param position New position of the track in milliseconds
   */
  void setPosition(long position);

  /**
   * @param marker Track position marker to place
   */
  void setMarker(TrackMarker marker);

  /**
   * @return Duration of the track in milliseconds
   */
  long getDuration();

  /**
   * @return Clone of this track which does not share the execution state of this track
   */
  AudioTrack makeClone();

  /**
   * @return The source manager which created this track. Null if not created by a source manager directly.
   */
  AudioSourceManager getSourceManager();

  /**
   * Attach an object with this track which can later be retrieved with {@link #getUserData()}. Useful for retrieving
   * application-specific object from the track in callbacks.
   *
   * @param userData Object to store.
   */
  void setUserData(Object userData);

  /**
   * @return Object previously stored with {@link #setUserData(Object)}
   */
  Object getUserData();

  /**
   * @param klass The expected class of the user data (or a superclass of it).
   * @return Object previously stored with {@link #setUserData(Object)} if it is of the specified type. If it is set,
   *         but with a different type, null is returned.
   */
  <T> T getUserData(Class<T> klass);
}
