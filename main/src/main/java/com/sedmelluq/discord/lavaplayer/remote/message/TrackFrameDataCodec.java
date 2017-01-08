package com.sedmelluq.discord.lavaplayer.remote.message;

import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;

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
  public int version() {
    return 1;
  }

  @Override
  public void encode(DataOutput out, TrackFrameDataMessage message) throws IOException {
    out.writeLong(message.executorId);
    out.writeInt(message.frames.size());

    for (AudioFrame frame : message.frames) {
      out.writeLong(frame.timecode);
      out.writeInt(frame.data.length);
      out.write(frame.data);
      out.writeInt(frame.volume);
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

      frames.add(new AudioFrame(timecode, data, in.readInt()));
    }

    return new TrackFrameDataMessage(executorId, frames, in.readBoolean(), in.readLong());
  }
}
