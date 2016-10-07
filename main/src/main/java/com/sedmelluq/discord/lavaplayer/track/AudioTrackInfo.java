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
   * @param title Track title
   * @param author Track author, if known
   * @param length Length of the track in milliseconds
   */
  public AudioTrackInfo(String title, String author, int length) {
    this.title = title;
    this.author = author;
    this.length = length;
  }
}
