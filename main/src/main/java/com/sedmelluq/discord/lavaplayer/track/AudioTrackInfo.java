package com.sedmelluq.discord.lavaplayer.track;

/**
 * Meta info for an audio track
 */
public class AudioTrackInfo {
  /**
   * Track title
   */
  public final String title;
  /**
   * Track author, if known
   */
  public final String author;
  /**
   * Length of the track in seconds
   */
  public final int lengthInSeconds;

  /**
   * @param title Track title
   * @param author Track author, if known
   * @param lengthInSeconds Length of the track in seconds
   */
  public AudioTrackInfo(String title, String author, int lengthInSeconds) {
    this.title = title;
    this.author = author;
    this.lengthInSeconds = lengthInSeconds;
  }
}
