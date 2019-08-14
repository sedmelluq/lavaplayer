package com.sedmelluq.discord.lavaplayer.tools;

/**
 * Decoded serialized exception. The original exception class is not restored, instead all exceptions will be instances
 * of this class and contain the original class name and message as fields and as the message.
 */
public class DecodedException extends Exception {
  /**
   * Original exception class name
   */
  public final String className;
  /**
   * Original exception message
   */
  public final String originalMessage;

  /**
   * @param className Original exception class name
   * @param originalMessage Original exception message
   * @param cause Cause of this exception
   */
  public DecodedException(String className, String originalMessage, DecodedException cause) {
    super(className + ": " + originalMessage, cause, true, true);

    this.className = className;
    this.originalMessage = originalMessage;
  }
}
