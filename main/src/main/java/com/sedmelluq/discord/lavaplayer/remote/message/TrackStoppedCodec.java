package com.sedmelluq.discord.lavaplayer.remote.message;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Codec for stopped track notification message.
 */
public class TrackStoppedCodec implements RemoteMessageCodec<TrackStoppedMessage> {
  @Override
  public Class<TrackStoppedMessage> getMessageClass() {
    return TrackStoppedMessage.class;
  }

  @Override
  public void encode(DataOutput out, TrackStoppedMessage message) throws IOException {
    out.writeLong(message.executorId);
  }

  @Override
  public TrackStoppedMessage decode(DataInput in) throws IOException {
    return new TrackStoppedMessage(in.readLong());
  }
}
