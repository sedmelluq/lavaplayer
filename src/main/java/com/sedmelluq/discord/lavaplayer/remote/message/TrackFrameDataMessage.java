package com.sedmelluq.discord.lavaplayer.remote.message;

import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;

import java.util.List;

/**
 * Message from an audio node to the master as a response to TrackFrameRequestMessage. Bouncing back the seeked position
 * is necessary because then the master can clear up the pending seek only if it matches this number. Otherwise the seek
 * position has been changed while the data for the previous seek was requested. The master also cannot clear the seek
 * state when sending the request, because in case the request fails, the seek will be discarded.
 */
public class TrackFrameDataMessage implements RemoteMessage {
  /**
   * The ID for the track executor
   */
  public final long executorId;
  /**
   * Frames provided by the node. These are missing the audio format, which must be attached locally. It can be assumed
   * that the node provides data in the format that it was initially requested in.
   */
  public final List<AudioFrame> frames;
  /**
   * If these are the last frames for the track. After receiving a message with this set to true, no more requests about
   * this track should be made to the node as it has already deleted the track from its registry.
   */
  public final boolean finished;
  /**
   * In case the data request included a seek, then this will report that the seek was completed by having the requested
   * seek position as a value. When no seek was performed, this is -1. The frames returned in this message start from
   * this position.
   */
  public final long seekedPosition;

  /**
   * @param executorId The ID for the track executor
   * @param frames Frames provided by the node
   * @param finished If these are the last frames for the track
   * @param seekedPosition The position of the seek that was performed
   */
  public TrackFrameDataMessage(long executorId, List<AudioFrame> frames, boolean finished, long seekedPosition) {
    this.executorId = executorId;
    this.frames = frames;
    this.finished = finished;
    this.seekedPosition = seekedPosition;
  }
}
