package com.sedmelluq.discord.lavaplayer.remote.message;

/**
 * This is the response to a TrackStartRequestMessage. It indicates whether the track was successfully started on the
 * node. This does not guarantee that any frames from the track will arrive, only that its execution was submitted.
 */
public class TrackStartResponseMessage implements RemoteMessage {
  /**
   * The ID for the track executor
   */
  public final long executorId;
  /**
   * Whether the track was successfully started in the node
   */
  public final boolean success;
  /**
   * The reason in case the track was not started
   */
  public final String failureReason;

  /**
   * @param executorId The ID for the track executor
   * @param success Whether the track was successfully started in the node
   * @param failureReason The reason in case the track was not started
   */
  public TrackStartResponseMessage(long executorId, boolean success, String failureReason) {
    this.executorId = executorId;
    this.success = success;
    this.failureReason = failureReason;
  }
}
