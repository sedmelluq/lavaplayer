package com.sedmelluq.discord.lavaplayer.remote.message;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;

/**
 * Track exception message which is sent by the node when processing the track in the node fails.
 */
public class TrackExceptionMessage implements RemoteMessage {
  /**
   * The ID for the track executor
   */
  public final long executorId;
  /**
   * Exception that was thrown by the local executor
   */
  public final FriendlyException exception;

  /**
   * @param executorId The ID for the track executor
   * @param exception Exception that was thrown by the local executor
   */
  public TrackExceptionMessage(long executorId, FriendlyException exception) {
    this.executorId = executorId;
    this.exception = exception;
  }
}
