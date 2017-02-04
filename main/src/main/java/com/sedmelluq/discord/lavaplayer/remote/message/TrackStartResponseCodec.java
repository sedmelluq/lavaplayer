package com.sedmelluq.discord.lavaplayer.remote.message;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Codec for track start request response message.
 */
public class TrackStartResponseCodec implements RemoteMessageCodec<TrackStartResponseMessage> {
  @Override
  public Class<TrackStartResponseMessage> getMessageClass() {
    return TrackStartResponseMessage.class;
  }

  @Override
  public int version(RemoteMessage message) {
    return 1;
  }

  @Override
  public void encode(DataOutput out, TrackStartResponseMessage message) throws IOException {
    out.writeLong(message.executorId);
    out.writeBoolean(message.success);

    if (!message.success) {
      out.writeUTF(message.failureReason);
    }
  }

  @Override
  public TrackStartResponseMessage decode(DataInput in, int version) throws IOException {
    long executorId = in.readLong();
    boolean success = in.readBoolean();

    return new TrackStartResponseMessage(executorId, success, success ? null : in.readUTF());
  }
}
