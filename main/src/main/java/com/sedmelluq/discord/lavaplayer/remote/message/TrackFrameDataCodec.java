package com.sedmelluq.discord.lavaplayer.remote.message;

import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.ImmutableAudioFrame;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Codec for track frame data message.
 */
public class TrackFrameDataCodec implements RemoteMessageCodec<TrackFrameDataMessage> {
  @Override
  public Class<TrackFrameDataMessage> getMessageClass() {
    return TrackFrameDataMessage.class;
  }

  @Override
  public int version(RemoteMessage message) {
    return 1;
  }

  @Override
  public void encode(DataOutput out, TrackFrameDataMessage message) throws IOException {
    out.writeLong(message.executorId);
    out.writeInt(message.frames.size());

    for (AudioFrame frame : message.frames) {
      out.writeLong(frame.getTimecode());
      out.writeInt(frame.getDataLength());
      out.write(frame.getData());
      out.writeInt(frame.getVolume());
    }

    out.writeBoolean(message.finished);
    out.writeLong(message.seekedPosition);
  }

  @Override
  public TrackFrameDataMessage decode(DataInput in, int version) throws IOException {
    long executorId = in.readLong();
    int frameCount = in.readInt();

    List<AudioFrame> frames = new ArrayList<>(frameCount);

    for (int i = 0; i < frameCount; i++) {
      long timecode = in.readLong();
      byte[] data = new byte[in.readInt()];
      in.readFully(data);

      frames.add(new ImmutableAudioFrame(timecode, data, in.readInt(), null));
    }

    return new TrackFrameDataMessage(executorId, frames, in.readBoolean(), in.readLong());
  }
}
