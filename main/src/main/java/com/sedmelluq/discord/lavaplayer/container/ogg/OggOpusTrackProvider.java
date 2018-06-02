package com.sedmelluq.discord.lavaplayer.container.ogg;

import com.sedmelluq.discord.lavaplayer.container.common.OpusPacketRouter;
import com.sedmelluq.discord.lavaplayer.tools.io.DirectBufferStreamBroker;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * OGG stream handler for Opus codec.
 */
public class OggOpusTrackProvider implements OggTrackProvider {
  private static final int OPUS_TAG_HALF = ByteBuffer.wrap(new byte[] { 'O', 'p', 'u', 's' }).getInt();
  private static final int TAGS_TAG_HALF = ByteBuffer.wrap(new byte[] { 'T', 'a', 'g', 's' }).getInt();

  private final OggPacketInputStream packetInputStream;
  private final DirectBufferStreamBroker broker;
  private final int channelCount;
  private final int sampleRate;
  private OpusPacketRouter opusPacketRouter;
  private ByteBuffer tagBuffer;

  /**
   * @param packetInputStream OGG packet input stream
   * @param broker Broker for loading stream data into direct byte buffer.
   * @param channelCount Number of channels in the track.
   * @param sampleRate Sample rate of the track.
   * @param tagBuffer Buffer containing the metadata tags section of the track.
   */
  public OggOpusTrackProvider(OggPacketInputStream packetInputStream, DirectBufferStreamBroker broker, int channelCount,
                              int sampleRate, ByteBuffer tagBuffer) {

    this.packetInputStream = packetInputStream;
    this.broker = broker;
    this.channelCount = channelCount;
    this.sampleRate = sampleRate;
    this.tagBuffer = tagBuffer;
  }

  @Override
  public void initialise(AudioProcessingContext context) throws IOException {
    opusPacketRouter = new OpusPacketRouter(context, sampleRate, channelCount);
  }

  @Override
  public OggMetadata getMetadata() {
    if (tagBuffer.getInt() != OPUS_TAG_HALF || tagBuffer.getInt() != TAGS_TAG_HALF) {
      return OggMetadata.EMPTY;
    }

    Map<String, String> tags = new HashMap<>();

    int vendorLength = Integer.reverseBytes(tagBuffer.getInt());
    tagBuffer.position(tagBuffer.position() + vendorLength);

    int itemCount = Integer.reverseBytes(tagBuffer.getInt());

    for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
      int itemLength = Integer.reverseBytes(tagBuffer.getInt());
      byte[] data = new byte[itemLength];

      for (int i = 0; i < data.length; i++) {
        if (data[i] == '=') {
          tags.put(new String(data, 0, i, UTF_8), new String(data, i, data.length - i, UTF_8));
          break;
        }
      }
    }

    return new OggMetadata(tags);
  }

  @Override
  public OggStreamSizeInfo seekForSizeInfo() throws IOException {
    return packetInputStream.seekForSizeInfo(sampleRate);
  }

  @Override
  public void provideFrames() throws InterruptedException {
    try {
      while (packetInputStream.startNewPacket()) {
        broker.consume(true, packetInputStream);

        ByteBuffer buffer = broker.getBuffer();

        if (buffer.remaining() > 0) {
          opusPacketRouter.process(buffer);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void seekToTimecode(long timecode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    if (opusPacketRouter != null) {
      opusPacketRouter.close();
    }
  }
}
