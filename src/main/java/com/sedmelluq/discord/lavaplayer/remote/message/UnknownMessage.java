package com.sedmelluq.discord.lavaplayer.remote.message;

/**
 * Used for cases where the message could not be decoded.
 */
public class UnknownMessage implements RemoteMessage {
  /**
   * Keep a singleton instance as there can be no difference between instances.
   */
  public static final UnknownMessage INSTANCE = new UnknownMessage();
}
