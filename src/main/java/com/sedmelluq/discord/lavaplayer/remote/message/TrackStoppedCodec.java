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
  public int version(RemoteMessage message) {
    return 1;
  }

  @Override
  public void encode(DataOutput out, TrackStoppedMessage message) throws IOException {
    out.writeLong(message.executorId);
  }

  @Override
  public TrackStoppedMessage decode(DataInput in, int version) throws IOException {
    return new TrackStoppedMessage(in.readLong());
  }
}
