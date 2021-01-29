package com.sedmelluq.discord.lavaplayer.container.ogg.vorbis;

import com.sedmelluq.discord.lavaplayer.container.ogg.OggCodecHandler;
import com.sedmelluq.discord.lavaplayer.container.ogg.OggMetadata;
import com.sedmelluq.discord.lavaplayer.container.ogg.OggPacketInputStream;
import com.sedmelluq.discord.lavaplayer.container.ogg.OggTrackBlueprint;
import com.sedmelluq.discord.lavaplayer.container.ogg.OggTrackHandler;
import com.sedmelluq.discord.lavaplayer.tools.io.DirectBufferStreamBroker;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class OggVorbisCodecHandler implements OggCodecHandler {
  private static final int VORBIS_IDENTIFIER = ByteBuffer.wrap(new byte[] { 0x01, 'v', 'o', 'r' }).getInt();

  // These are arbitrary - there is no limit specified in Vorbis specification, Opus limit used as reference.
  private static final int MAX_COMMENTS_SAVED_LENGTH = 1024 * 128; // 128 KB
  private static final int MAX_COMMENTS_READ_LENGTH = 1024 * 1024 * 120; // 120 MB

  private static final byte[] COMMENT_PACKET_START = new byte[] { 0x03, 'v', 'o', 'r', 'b', 'i', 's' };

  @Override
  public boolean isMatchingIdentifier(int identifier) {
    return identifier == VORBIS_IDENTIFIER;
  }

  @Override
  public int getMaximumFirstPacketLength() {
    return 64;
  }

  @Override
  public OggTrackBlueprint loadBlueprint(OggPacketInputStream stream, DirectBufferStreamBroker broker) throws IOException {
    byte[] infoPacket = broker.extractBytes();
    loadCommentsHeader(stream, broker, true);
    return new Blueprint(infoPacket, broker);
  }

  @Override
  public OggMetadata loadMetadata(OggPacketInputStream stream, DirectBufferStreamBroker broker) throws IOException {
    loadCommentsHeader(stream, broker, false);

    ByteBuffer commentsPacket = broker.getBuffer();
    byte[] packetStart = new byte[COMMENT_PACKET_START.length];
    commentsPacket.get(packetStart);

    if (!Arrays.equals(packetStart, COMMENT_PACKET_START)) {
      return OggMetadata.EMPTY;
    }

    return new OggMetadata(VorbisCommentParser.parse(commentsPacket, broker.isTruncated()), null);
  }

  private void loadCommentsHeader(OggPacketInputStream stream, DirectBufferStreamBroker broker, boolean skip)
      throws IOException {

    if (!stream.startNewPacket()) {
      throw new IllegalStateException("No comments packet in track.");
    } else if (!broker.consumeNext(stream, skip ? 0 : MAX_COMMENTS_SAVED_LENGTH, MAX_COMMENTS_READ_LENGTH)) {
      if (!stream.isPacketComplete()) {
        throw new IllegalStateException("Vorbis comments header packet longer than allowed.");
      }
    }
  }

  private static class Blueprint implements OggTrackBlueprint {
    private final byte[] infoPacket;
    private final DirectBufferStreamBroker broker;

    private Blueprint(byte[] infoPacket, DirectBufferStreamBroker broker) {
      this.infoPacket = infoPacket;
      this.broker = broker;
    }

    @Override
    public OggTrackHandler loadTrackHandler(OggPacketInputStream stream) {
      return new OggVorbisTrackHandler(infoPacket, stream, broker);
    }
  }
}
