package com.sedmelluq.discord.lavaplayer.container.ogg;

import com.sedmelluq.discord.lavaplayer.filter.AudioPipeline;
import com.sedmelluq.discord.lavaplayer.filter.AudioPipelineFactory;
import com.sedmelluq.discord.lavaplayer.filter.PcmFormat;
import com.sedmelluq.discord.lavaplayer.natives.vorbis.VorbisDecoder;
import com.sedmelluq.discord.lavaplayer.tools.io.DirectBufferStreamBroker;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * OGG stream handler for Vorbis codec.
 */
public class OggVorbisTrackProvider implements OggTrackProvider {
  private static final int PCM_BUFFER_SIZE = 4096;

  private final OggPacketInputStream packetInputStream;
  private final DirectBufferStreamBroker broker;
  private final VorbisDecoder decoder;
  private final int sampleRate;
  private float[][] channelPcmBuffers;
  private AudioPipeline downstream;

  /**
   * @param packetInputStream OGG packet input stream
   * @param broker Broker for loading stream data into direct byte buffer, it has already loaded the first packet of the
   *               stream at this point.
   */
  public OggVorbisTrackProvider(OggPacketInputStream packetInputStream, DirectBufferStreamBroker broker) {
    this.packetInputStream = packetInputStream;
    this.broker = broker;
    this.decoder = new VorbisDecoder();
    this.sampleRate =  Integer.reverseBytes(broker.getBuffer().getInt(12));

    int channelCount = broker.getBuffer().get(11) & 0xFF;
    channelPcmBuffers = new float[channelCount][];

    for (int i = 0; i < channelPcmBuffers.length; i++) {
      channelPcmBuffers[i] = new float[PCM_BUFFER_SIZE];
    }
  }

  @Override
  public void initialise(AudioProcessingContext context) throws IOException {
    passHeader(0);
    passHeader(1);
    passHeader(2);

    decoder.initialise();
    broker.resetAndCompact();

    downstream = AudioPipelineFactory.create(context, new PcmFormat(decoder.getChannelCount(), sampleRate));
  }

  @Override
  public OggMetadata getMetadata() {
    return OggMetadata.EMPTY;
  }

  @Override
  public OggStreamSizeInfo seekForSizeInfo() throws IOException {
    return null;
  }

  private void passHeader(int index) throws IOException {
    if (index > 0) {
      if (!packetInputStream.startNewPacket()) {
        throw new IllegalStateException("End of track before header " + index + " .");
      }

      broker.consume(true, packetInputStream);
    }

    ByteBuffer headerBuffer = broker.getBuffer();
    decoder.parseHeader(headerBuffer, headerBuffer.limit(), index == 0);
  }

  @Override
  public void provideFrames() throws InterruptedException {
    try {
      while (packetInputStream.startNewPacket()) {
        broker.consume(true, packetInputStream);
        provideFromBuffer(broker.getBuffer());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void provideFromBuffer(ByteBuffer buffer) throws InterruptedException {
    decoder.input(buffer);
    int output;

    do {
      output = decoder.output(channelPcmBuffers);

      if (output > 0) {
        downstream.process(channelPcmBuffers, 0, output);
      }
    } while (output == PCM_BUFFER_SIZE);
  }

  @Override
  public void seekToTimecode(long timecode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    if (downstream != null) {
      downstream.close();
    }

    decoder.close();
  }
}
