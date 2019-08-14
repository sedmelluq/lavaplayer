package com.sedmelluq.discord.lavaplayer.remote.message;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Codec for node statistics message.
 */
public class NodeStatisticsCodec implements RemoteMessageCodec<NodeStatisticsMessage> {
  @Override
  public Class<NodeStatisticsMessage> getMessageClass() {
    return NodeStatisticsMessage.class;
  }

  @Override
  public int version(RemoteMessage message) {
    return 1;
  }

  @Override
  public void encode(DataOutput out, NodeStatisticsMessage message) throws IOException {
    out.writeInt(message.playingTrackCount);
    out.writeInt(message.totalTrackCount);
    out.writeFloat(message.systemCpuUsage);
    out.writeFloat(message.processCpuUsage);
  }

  @Override
  public NodeStatisticsMessage decode(DataInput in, int version) throws IOException {
    return new NodeStatisticsMessage(in.readInt(), in.readInt(), in.readFloat(), in.readFloat());
  }
}
