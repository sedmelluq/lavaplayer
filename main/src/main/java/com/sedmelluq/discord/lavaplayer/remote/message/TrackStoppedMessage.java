package com.sedmelluq.discord.lavaplayer.remote.message;

/**
 * Message to notify the node that the track has been stopped in the master. No more requests for that track will occur
 * so the track may be deleted from the node.
 */
public class TrackStoppedMessage implements RemoteMessage {
  /**
   * The ID for the track executor
   */
  public final long executorId;

  /**
   * @param executorId The ID for the track executor
   */
  public TrackStoppedMessage(long executorId) {
    this.executorId = executorId;
  }
}
