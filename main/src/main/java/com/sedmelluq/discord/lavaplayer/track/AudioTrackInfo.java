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
   * Length of the track in milliseconds
   */
  public final int length;
  /**
   * Audio source specific track identifier
   */
  public final String identifier;

  /**
   * @param title Track title
   * @param author Track author, if known
   * @param length Length of the track in milliseconds
   * @param identifier Audio source specific track identifier
   */
  public AudioTrackInfo(String title, String author, int length, String identifier) {
    this.title = title;
    this.author = author;
    this.length = length;
    this.identifier = identifier;
  }
}
