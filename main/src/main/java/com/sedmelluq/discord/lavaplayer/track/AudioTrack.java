package com.sedmelluq.discord.lavaplayer.track;

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
   * @param loop Descriptor for the loop to set
   */
  void setLoop(AudioLoop loop);

  /**
   * @return Duration of the track in milliseconds
   */
  long getDuration();
}
