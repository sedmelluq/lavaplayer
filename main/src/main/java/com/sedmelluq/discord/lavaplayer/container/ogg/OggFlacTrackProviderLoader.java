package com.sedmelluq.discord.lavaplayer.container.ogg;

import com.sedmelluq.discord.lavaplayer.container.flac.FlacMetadataReader;
import com.sedmelluq.discord.lavaplayer.container.flac.FlacStreamInfo;
import com.sedmelluq.discord.lavaplayer.container.flac.FlacTrackInfo;
import com.sedmelluq.discord.lavaplayer.container.flac.FlacTrackInfoBuilder;
import com.sedmelluq.discord.lavaplayer.tools.io.ByteBufferInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.DirectBufferStreamBroker;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Loader for an OGG FLAC track from an OGG packet stream.
 */
public class OggFlacTrackProviderLoader {
  private static final int NATIVE_FLAC_HEADER = ByteBuffer.wrap(new byte[] { 'f', 'L', 'a', 'C' }).getInt();

  /**
   * @param packetInputStream OGG packet input stream
   * @param broker Broker for loading stream data into direct byte buffer, it has already loaded the first packet of the
   *               stream at this point.
   * @return An OGG FLAC track frame provider.
   * @throws IOException On read error.
   */
  public static OggFlacTrackProvider load(OggPacketInputStream packetInputStream, DirectBufferStreamBroker broker) throws IOException {
    ByteBuffer buffer = broker.getBuffer();

    if (buffer.getInt(9) != NATIVE_FLAC_HEADER) {
      throw new IllegalStateException("Native flac header not found.");
    }

    buffer.position(13);

    FlacTrackInfo trackInfo = readHeaders(buffer, packetInputStream);
    return new OggFlacTrackProvider(trackInfo, packetInputStream);
  }

  private static FlacTrackInfo readHeaders(ByteBuffer firstPacketBuffer, OggPacketInputStream packetInputStream) throws IOException {
    FlacStreamInfo streamInfo = FlacMetadataReader.readStreamInfoBlock(new DataInputStream(new ByteBufferInputStream(firstPacketBuffer)));
    FlacTrackInfoBuilder trackInfoBuilder = new FlacTrackInfoBuilder(streamInfo);

    DataInputStream dataInputStream = new DataInputStream(packetInputStream);

    boolean hasMoreMetadata = trackInfoBuilder.getStreamInfo().hasMetadataBlocks;

    while (hasMoreMetadata) {
      if (!packetInputStream.startNewPacket()) {
        throw new IllegalStateException("Track ended when more metadata was expected.");
      }

      hasMoreMetadata = FlacMetadataReader.readMetadataBlock(dataInputStream, packetInputStream, trackInfoBuilder);
    }

    return trackInfoBuilder.build();
  }
}
