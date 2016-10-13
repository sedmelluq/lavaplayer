package com.sedmelluq.discord.lavaplayer.remote.message;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Codec for track start message.
 */
public class TrackStartRequestCodec implements RemoteMessageCodec<TrackStartRequestMessage> {
  @Override
  public Class<TrackStartRequestMessage> getMessageClass() {
    return TrackStartRequestMessage.class;
  }

  @Override
  public void encode(DataOutput out, TrackStartRequestMessage message) throws IOException {
    out.writeLong(message.executorId);
    out.writeUTF(message.trackInfo.title);
    out.writeUTF(message.trackInfo.author);
    out.writeInt(message.trackInfo.length);
    out.writeUTF(message.trackInfo.identifier);
    out.writeInt(message.encodedTrack.length);
    out.write(message.encodedTrack);
    out.writeInt(message.volume);
    out.writeUTF(message.configuration.getResamplingQuality().name());
    out.writeInt(message.configuration.getOpusEncodingQuality());
  }

  @Override
  public TrackStartRequestMessage decode(DataInput in) throws IOException {
    long executorId = in.readLong();
    AudioTrackInfo trackInfo = new AudioTrackInfo(in.readUTF(), in.readUTF(), in.readInt(), in.readUTF());

    byte[] encodedTrack = new byte[in.readInt()];
    in.readFully(encodedTrack);

    int volume = in.readInt();
    AudioConfiguration configuration = new AudioConfiguration();
    configuration.setResamplingQuality(AudioConfiguration.ResamplingQuality.valueOf(in.readUTF()));
    configuration.setOpusEncodingQuality(in.readInt());

    return new TrackStartRequestMessage(executorId, trackInfo, encodedTrack, volume, configuration);
  }
}
