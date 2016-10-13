package com.sedmelluq.discord.lavaplayer.remote.message;

/**
 * A message sent to the node to request frames from a track. This is sent even when no frames are required as it is
 * used to indicate that the track is still alive in the master.
 */
public class TrackFrameRequestMessage implements RemoteMessage {
  /**
   * The ID for the track executor
   */
  public final long executorId;
  /**
   * Maximum number of frames that can be included in the response
   */
  public final int maximumFrames;
  /**
   * Current volume of the track
   */
  public final int volume;
  /**
   * The position to seek to. Value is -1 if no seeking is required at the moment.
   */
  public final long seekPosition;

  /**
   * @param executorId The ID for the track executor
   * @param maximumFrames Maximum number of frames that can be included in the response
   * @param volume Current volume of the track
   * @param seekPosition The position to seek to
   */
  public TrackFrameRequestMessage(long executorId, int maximumFrames, int volume, long seekPosition) {
    this.executorId = executorId;
    this.maximumFrames = maximumFrames;
    this.volume = volume;
    this.seekPosition = seekPosition;
  }
}
