package com.sedmelluq.discord.lavaplayer.track.info;

/**
 * Provider for audio track info.
 */
public interface AudioTrackInfoProvider {
  /**
   * @return Track title, or <code>null</code> if this provider does not know it.
   */
  String getTitle();

  /**
   * @return Track author, or <code>null</code> if this provider does not know it.
   */
  String getAuthor();

  /**
   * @return Track length in milliseconds, or <code>null</code> if this provider does not know it.
   */
  Long getLength();

  /**
   * @return Track identifier, or <code>null</code> if this provider does not know it.
   */
  String getIdentifier();

  /**
   * @return Track URI, or <code>null</code> if this provider does not know it.
   */
  String getUri();
}
