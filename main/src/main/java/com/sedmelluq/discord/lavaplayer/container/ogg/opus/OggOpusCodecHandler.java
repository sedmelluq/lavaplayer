package com.sedmelluq.discord.lavaplayer.container.ogg.opus;

import com.sedmelluq.discord.lavaplayer.container.ogg.OggCodecHandler;
import com.sedmelluq.discord.lavaplayer.container.ogg.OggMetadata;
import com.sedmelluq.discord.lavaplayer.container.ogg.OggPacketInputStream;
import com.sedmelluq.discord.lavaplayer.container.ogg.OggStreamSizeInfo;
import com.sedmelluq.discord.lavaplayer.container.ogg.OggTrackBlueprint;
import com.sedmelluq.discord.lavaplayer.container.ogg.OggTrackHandler;
import com.sedmelluq.discord.lavaplayer.container.ogg.vorbis.VorbisCommentParser;
import com.sedmelluq.discord.lavaplayer.tools.io.DirectBufferStreamBroker;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

/**
 * Loader for Opus track providers from an OGG stream.
 */
public class OggOpusCodecHandler implements OggCodecHandler {
  private static final int OPUS_IDENTIFIER = ByteBuffer.wrap(new byte[] { 'O', 'p', 'u', 's' }).getInt();
  private static final int HEAD_TAG_HALF = ByteBuffer.wrap(new byte[] { 'H', 'e', 'a', 'd' }).getInt();

  private static final int OPUS_TAG_HALF = ByteBuffer.wrap(new byte[] { 'O', 'p', 'u', 's' }).getInt();
  private static final int TAGS_TAG_HALF = ByteBuffer.wrap(new byte[] { 'T', 'a', 'g', 's' }).getInt();

  private static final int MAX_COMMENTS_SAVED_LENGTH = 1024 * 60; // 60 KB
  private static final int MAX_COMMENTS_READ_LENGTH = 1024 * 1024 * 120; // 120 MB

  @Override
  public boolean isMatchingIdentifier(int identifier) {
    return identifier == OPUS_IDENTIFIER;
  }

  @Override
  public int getMaximumFirstPacketLength() {
    return 276;
  }

  @Override
  public OggTrackBlueprint loadBlueprint(OggPacketInputStream stream, DirectBufferStreamBroker broker) throws IOException {
    ByteBuffer firstPacket = broker.getBuffer();
    verifyFirstPacket(firstPacket);

    loadCommentsHeader(stream, broker, true);

    int channelCount = firstPacket.get(9) & 0xFF;
    return new Blueprint(broker, channelCount, getSampleRate(firstPacket));
  }

  @Override
  public OggMetadata loadMetadata(OggPacketInputStream stream, DirectBufferStreamBroker broker) throws IOException {
    ByteBuffer firstPacket = broker.getBuffer();
    verifyFirstPacket(firstPacket);

    loadCommentsHeader(stream, broker, false);

    return new OggMetadata(
        parseTags(broker.getBuffer(), broker.isTruncated()),
        detectLength(stream, getSampleRate(firstPacket))
    );
  }

  private Map<String, String> parseTags(ByteBuffer tagBuffer, boolean truncated) {
    if (tagBuffer.getInt() != OPUS_TAG_HALF || tagBuffer.getInt() != TAGS_TAG_HALF) {
      return Collections.emptyMap();
    }

    return VorbisCommentParser.parse(tagBuffer, truncated);
  }

  private Long detectLength(OggPacketInputStream stream, int sampleRate) throws IOException {
    OggStreamSizeInfo sizeInfo = stream.seekForSizeInfo(sampleRate);

    if (sizeInfo != null) {
      return sizeInfo.totalSamples * 1000 / sizeInfo.sampleRate;
    } else {
      return null;
    }
  }

  private void verifyFirstPacket(ByteBuffer firstPacket) {
    if (firstPacket.getInt(4) != HEAD_TAG_HALF) {
      throw new IllegalStateException("First packet is not an OpusHead.");
    }
  }

  private int getSampleRate(ByteBuffer firstPacket) {
    return Integer.reverseBytes(firstPacket.getInt(12));
  }

  private void loadCommentsHeader(OggPacketInputStream stream, DirectBufferStreamBroker broker, boolean skip)
      throws IOException {

    if (!stream.startNewPacket()) {
      throw new IllegalStateException("No OpusTags packet in track.");
    } else if (!broker.consumeNext(stream, skip ? 0 : MAX_COMMENTS_SAVED_LENGTH, MAX_COMMENTS_READ_LENGTH)) {
      if (!stream.isPacketComplete()) {
        throw new IllegalStateException("Opus comments header packet longer than allowed.");
      }
    }
  }

  private static class Blueprint implements OggTrackBlueprint {
    private final DirectBufferStreamBroker broker;
    private final int channelCount;
    private final int sampleRate;

    private Blueprint(DirectBufferStreamBroker broker, int channelCount, int sampleRate) {
      this.broker = broker;
      this.channelCount = channelCount;
      this.sampleRate = sampleRate;
    }

    @Override
    public OggTrackHandler loadTrackHandler(OggPacketInputStream stream) {
      broker.clear();
      return new OggOpusTrackHandler(stream, broker, channelCount, sampleRate);
    }
  }
}
