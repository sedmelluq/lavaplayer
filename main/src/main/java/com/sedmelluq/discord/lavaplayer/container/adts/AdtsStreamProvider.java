package com.sedmelluq.discord.lavaplayer.container.adts;

import com.sedmelluq.discord.lavaplayer.filter.AudioPipeline;
import com.sedmelluq.discord.lavaplayer.filter.AudioPipelineFactory;
import com.sedmelluq.discord.lavaplayer.filter.PcmFormat;
import com.sedmelluq.discord.lavaplayer.natives.aac.AacDecoder;
import com.sedmelluq.discord.lavaplayer.tools.io.DirectBufferStreamBroker;
import com.sedmelluq.discord.lavaplayer.tools.io.ResettableBoundedInputStream;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Provides the frames of an ADTS stream track to the frame consumer.
 */
public class AdtsStreamProvider {
  private final AudioProcessingContext context;
  private final AdtsStreamReader streamReader;
  private final AacDecoder decoder;
  private final ResettableBoundedInputStream packetBoundedStream;
  private final DirectBufferStreamBroker directBufferBroker;
  private ShortBuffer outputBuffer;
  private AdtsPacketHeader previousHeader;
  private AudioPipeline downstream;

  /**
   * @param inputStream Input stream to read from.
   * @param context Audio processing context.
   */
  public AdtsStreamProvider(InputStream inputStream, AudioProcessingContext context) {
    this.context = context;
    this.streamReader = new AdtsStreamReader(inputStream);
    this.decoder = new AacDecoder();
    this.packetBoundedStream = new ResettableBoundedInputStream(inputStream);
    this.directBufferBroker = new DirectBufferStreamBroker(2048);
  }

  /**
   * Provides frames to the frame consumer.
   * @throws InterruptedException
   */
  public void provideFrames() throws InterruptedException {
    try {
      while (true) {
        AdtsPacketHeader header = streamReader.findPacketHeader();
        if (header == null) {
          // Reached EOF while scanning for header
          return;
        }

        configureProcessing(header);

        packetBoundedStream.resetLimit(header.payloadLength);
        directBufferBroker.consume(true, packetBoundedStream);

        ByteBuffer buffer = directBufferBroker.getBuffer();

        if (buffer.limit() < header.payloadLength) {
          // Reached EOF in the middle of a packet
          return;
        }

        decodeAndSend(buffer);
        streamReader.nextPacket();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void decodeAndSend(ByteBuffer inputBuffer) throws InterruptedException {
    decoder.fill(inputBuffer);

    if (downstream == null) {
      AacDecoder.StreamInfo streamInfo = decoder.resolveStreamInfo();
      if (streamInfo == null) {
        return;
      }

      downstream = AudioPipelineFactory.create(context, new PcmFormat(streamInfo.channels, streamInfo.sampleRate));
      outputBuffer = ByteBuffer.allocateDirect(2 * streamInfo.frameSize * streamInfo.channels)
          .order(ByteOrder.nativeOrder()).asShortBuffer();
    }

    outputBuffer.clear();

    while (decoder.decode(outputBuffer, false)) {
      downstream.process(outputBuffer);
      outputBuffer.clear();
    }
  }

  private void configureProcessing(AdtsPacketHeader header) {
    if (!header.canUseSameDecoder(previousHeader)) {
      decoder.configure(header.profile, header.sampleRate, header.channels);

      if (downstream != null) {
        downstream.close();
      }

      downstream = null;
      outputBuffer = null;
    }

    previousHeader = header;
  }

  /**
   * Free all resources.
   */
  public void close() {
    try {
      if (downstream != null) {
        downstream.close();
      }
    } finally {
      decoder.close();
    }
  }
}
