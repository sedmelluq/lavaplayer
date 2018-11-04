package com.sedmelluq.discord.lavaplayer.container.ogg;

import com.sedmelluq.discord.lavaplayer.tools.io.DirectBufferStreamBroker;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Track loader for an OGG packet stream. Automatically detects the track codec and loads the specific track handler.
 */
public class OggTrackLoader {
  private static final int FLAC_IDENTIFIER = ByteBuffer.wrap(new byte[] { 0x7F, 'F', 'L', 'A' }).getInt();
  private static final int VORBIS_IDENTIFIER = ByteBuffer.wrap(new byte[] { 0x01, 'v', 'o', 'r' }).getInt();
  private static final int OPUS_IDENTIFIER = ByteBuffer.wrap(new byte[] { 'O', 'p', 'u', 's' }).getInt();

  /**
   * @param packetInputStream OGG packet input stream
   * @return The track handler detected from this packet input stream. Returns null if the stream ended.
   * @throws IOException On read error
   * @throws IllegalStateException If the track uses an unknown codec.
   */
  public static OggTrackProvider loadTrack(OggPacketInputStream packetInputStream) throws IOException {
    if (!packetInputStream.startNewTrack() || !packetInputStream.startNewPacket()) {
      return null;
    }

    DirectBufferStreamBroker broker = new DirectBufferStreamBroker(1024);
    broker.consume(true, packetInputStream);

    int headerIdentifier = broker.getBuffer().getInt();
    return chooseTrackFromIdentifier(headerIdentifier, packetInputStream, broker);
  }

  private static OggTrackProvider chooseTrackFromIdentifier(int headerIdentifier, OggPacketInputStream packetInputStream,
                                                            DirectBufferStreamBroker broker) throws IOException {

    if (headerIdentifier == FLAC_IDENTIFIER) {
      return OggFlacTrackProviderLoader.load(packetInputStream, broker);
    } else if (headerIdentifier == VORBIS_IDENTIFIER) {
      return new OggVorbisTrackProvider(packetInputStream, broker);
    } else if (headerIdentifier == OPUS_IDENTIFIER) {
      return OggOpusTrackProviderLoader.load(packetInputStream, broker);
    } else {
      throw new IllegalStateException("Unsupported track in OGG stream.");
    }
  }
}
