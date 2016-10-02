package com.sedmelluq.discord.lavaplayer.tools;

/**
 * An exception with a friendly message.
 */
public class FriendlyException extends RuntimeException {

  /**
   * @param friendlyMessage A message which is understandable to end-users
   * @param cause The cause of the exception with technical details
   */
  public FriendlyException(String friendlyMessage, Throwable cause) {
    super(friendlyMessage, cause);
  }
}
