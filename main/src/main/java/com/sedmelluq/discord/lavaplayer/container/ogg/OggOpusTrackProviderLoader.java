package com.sedmelluq.discord.lavaplayer.container.ogg;

import com.sedmelluq.discord.lavaplayer.tools.io.DirectBufferStreamBroker;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Loader for Opus track providers from an OGG stream.
 */
public class OggOpusTrackProviderLoader {
  private static final int HEAD_TAG_HALF = ByteBuffer.wrap(new byte[] { 'H', 'e', 'a', 'd' }).getInt();

  /**
   * @param packetInputStream OGG packet input stream.
   * @param broker Broker for loading stream data into direct byte buffer.
   * @return An OGG Opus track frame provider.
   * @throws IOException On read error.
   */
  public static OggOpusTrackProvider load(OggPacketInputStream packetInputStream, DirectBufferStreamBroker broker)
      throws IOException {

    ByteBuffer buffer = broker.getBuffer();

    if (buffer.getInt(4) != HEAD_TAG_HALF) {
      throw new IllegalStateException("First packet is not an OpusHead.");
    }

    int channelCount = buffer.get(9) & 0xFF;
    int sampleRate = Integer.reverseBytes(buffer.getInt(12));

    if (!packetInputStream.startNewPacket()) {
      throw new IllegalStateException("No OpusTags packet in track.");
    }

    broker.consume(true, packetInputStream);

    ByteBuffer tagBuffer = ByteBuffer.allocate(broker.getBuffer().remaining());
    broker.getBuffer().get(tagBuffer.array());

    return new OggOpusTrackProvider(packetInputStream, broker, channelCount, sampleRate, tagBuffer);
  }
}
