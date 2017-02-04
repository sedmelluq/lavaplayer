package com.sedmelluq.discord.lavaplayer.remote.message;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Codec for track start message.
 */
public class TrackStartRequestCodec implements RemoteMessageCodec<TrackStartRequestMessage> {
  private static final int VERSION_WITH_FORMAT = 2;

  @Override
  public Class<TrackStartRequestMessage> getMessageClass() {
    return TrackStartRequestMessage.class;
  }

  @Override
  public int version(RemoteMessage message) {
    // Backwards compatibility with older nodes.
    if (message instanceof TrackStartRequestMessage) {
      AudioDataFormat format = ((TrackStartRequestMessage) message).configuration.getOutputFormat();

      if (format.equals(StandardAudioDataFormats.DISCORD_OPUS)) {
        return 1;
      }
    }

    return 2;
  }

  @Override
  public void encode(DataOutput out, TrackStartRequestMessage message) throws IOException {
    out.writeLong(message.executorId);
    out.writeUTF(message.trackInfo.title);
    out.writeUTF(message.trackInfo.author);
    out.writeLong(message.trackInfo.length);
    out.writeUTF(message.trackInfo.identifier);
    out.writeBoolean(message.trackInfo.isStream);
    out.writeInt(message.encodedTrack.length);
    out.write(message.encodedTrack);
    out.writeInt(message.volume);
    out.writeUTF(message.configuration.getResamplingQuality().name());
    out.writeInt(message.configuration.getOpusEncodingQuality());

    if (version(message) >= VERSION_WITH_FORMAT) {
      AudioDataFormat format = message.configuration.getOutputFormat();
      out.writeInt(format.channelCount);
      out.writeInt(format.sampleRate);
      out.writeInt(format.chunkSampleCount);
      out.writeUTF(format.codec.name());
    }
  }

  @Override
  public TrackStartRequestMessage decode(DataInput in, int version) throws IOException {
    long executorId = in.readLong();
    AudioTrackInfo trackInfo = new AudioTrackInfo(in.readUTF(), in.readUTF(), in.readLong(), in.readUTF(), in.readBoolean());

    byte[] encodedTrack = new byte[in.readInt()];
    in.readFully(encodedTrack);

    int volume = in.readInt();
    AudioConfiguration configuration = new AudioConfiguration();
    configuration.setResamplingQuality(AudioConfiguration.ResamplingQuality.valueOf(in.readUTF()));
    configuration.setOpusEncodingQuality(in.readInt());

    if (version >= VERSION_WITH_FORMAT) {
      AudioDataFormat format = new AudioDataFormat(in.readInt(), in.readInt(), in.readInt(),
          AudioDataFormat.Codec.valueOf(in.readUTF()));

      configuration.setOutputFormat(format);
    }

    return new TrackStartRequestMessage(executorId, trackInfo, encodedTrack, volume, configuration);
  }
}
