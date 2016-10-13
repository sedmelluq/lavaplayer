package com.sedmelluq.discord.lavaplayer.remote.message;

/**
 * All remote message types.
 */
public enum RemoteMessageType {
  TRACK_START_REQUEST(new TrackStartRequestCodec()),
  TRACK_START_RESPONSE(new TrackStartResponseCodec()),
  TRACK_FRAME_REQUEST(new TrackFrameRequestCodec()),
  TRACK_FRAME_DATA(new TrackFrameDataCodec()),
  TRACK_STOPPED(new TrackStoppedCodec()),
  TRACK_EXCEPTION(new TrackExceptionCodec()),
  NODE_STATISTICS(new NodeStatisticsCodec());

  /**
   * The codec used for encoding and decoding this type of message.
   */
  public final RemoteMessageCodec<?> codec;

  RemoteMessageType(RemoteMessageCodec<?> codec) {
    this.codec = codec;
  }
}
