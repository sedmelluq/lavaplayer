package com.sedmelluq.discord.lavaplayer.container.ogg.vorbis;

import com.sedmelluq.discord.lavaplayer.container.ogg.OggPacketInputStream;
import com.sedmelluq.discord.lavaplayer.container.ogg.OggTrackHandler;
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
public class OggVorbisTrackHandler implements OggTrackHandler {
  private static final int PCM_BUFFER_SIZE = 4096;

  private final byte[] infoPacket;
  private final OggPacketInputStream packetInputStream;
  private final DirectBufferStreamBroker broker;
  private final VorbisDecoder decoder;
  private final int sampleRate;
  private float[][] channelPcmBuffers;
  private AudioPipeline downstream;

  /**
   * @param packetInputStream OGG packet input stream
   * @param broker Broker for loading stream data into direct byte buffer, it has already loaded the first two packets
   *               (info and comments) and should be in the state where we should request the next - the setup packet.
   */
  public OggVorbisTrackHandler(byte[] infoPacket, OggPacketInputStream packetInputStream,
                               DirectBufferStreamBroker broker) {

    this.infoPacket = infoPacket;
    this.packetInputStream = packetInputStream;
    this.broker = broker;
    this.decoder = new VorbisDecoder();

    ByteBuffer infoBuffer = ByteBuffer.wrap(infoPacket);
    this.sampleRate =  Integer.reverseBytes(infoBuffer.getInt(12));

    int channelCount = infoBuffer.get(11) & 0xFF;
    channelPcmBuffers = new float[channelCount][];

    for (int i = 0; i < channelPcmBuffers.length; i++) {
      channelPcmBuffers[i] = new float[PCM_BUFFER_SIZE];
    }
  }

  @Override
  public void initialise(AudioProcessingContext context, long timecode, long desiredTimecode) throws IOException {
    ByteBuffer infoBuffer = ByteBuffer.allocateDirect(infoPacket.length);
    infoBuffer.put(infoPacket);
    infoBuffer.flip();

    if (!packetInputStream.startNewPacket()) {
      throw new IllegalStateException("End of track before header setup header.");
    }

    broker.consumeNext(packetInputStream, Integer.MAX_VALUE, Integer.MAX_VALUE);
    decoder.initialise(infoBuffer, broker.getBuffer());

    broker.resetAndCompact();

    downstream = AudioPipelineFactory.create(context, new PcmFormat(decoder.getChannelCount(), sampleRate));
    downstream.seekPerformed(desiredTimecode, timecode);
  }

  @Override
  public void provideFrames() throws InterruptedException {
    try {
      while (packetInputStream.startNewPacket()) {
        broker.consumeNext(packetInputStream, Integer.MAX_VALUE, Integer.MAX_VALUE);
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
