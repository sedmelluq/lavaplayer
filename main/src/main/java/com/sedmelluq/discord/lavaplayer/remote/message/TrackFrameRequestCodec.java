package com.sedmelluq.discord.lavaplayer.remote.message;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Codec for track frame request message.
 */
public class TrackFrameRequestCodec implements RemoteMessageCodec<TrackFrameRequestMessage> {
  @Override
  public Class<TrackFrameRequestMessage> getMessageClass() {
    return TrackFrameRequestMessage.class;
  }

  @Override
  public void encode(DataOutput out, TrackFrameRequestMessage message) throws IOException {
    out.writeLong(message.executorId);
    out.writeInt(message.maximumFrames);
    out.writeInt(message.volume);
    out.writeLong(message.seekPosition);
  }

  @Override
  public TrackFrameRequestMessage decode(DataInput in) throws IOException {
    return new TrackFrameRequestMessage(in.readLong(), in.readInt(), in.readInt(), in.readLong());
  }
}
