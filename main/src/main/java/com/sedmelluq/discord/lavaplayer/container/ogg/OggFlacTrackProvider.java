package com.sedmelluq.discord.lavaplayer.container.ogg;

import com.sedmelluq.discord.lavaplayer.container.flac.FlacTrackInfo;
import com.sedmelluq.discord.lavaplayer.container.flac.frame.FlacFrameReader;
import com.sedmelluq.discord.lavaplayer.filter.AudioPipeline;
import com.sedmelluq.discord.lavaplayer.filter.AudioPipelineFactory;
import com.sedmelluq.discord.lavaplayer.filter.PcmFormat;
import com.sedmelluq.discord.lavaplayer.tools.io.BitStreamReader;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.io.IOException;

/**
 * OGG stream handler for FLAC codec.
 */
public class OggFlacTrackProvider implements OggTrackProvider {
  private final FlacTrackInfo info;
  private final OggPacketInputStream packetInputStream;
  private final BitStreamReader bitStreamReader;
  private final int[] decodingBuffer;
  private final int[][] rawSampleBuffers;
  private final short[][] sampleBuffers;
  private AudioPipeline downstream;

  /**
   * @param info FLAC track info
   * @param packetInputStream OGG packet input stream
   */
  public OggFlacTrackProvider(FlacTrackInfo info, OggPacketInputStream packetInputStream) {
    this.info = info;
    this.packetInputStream = packetInputStream;
    this.bitStreamReader = new BitStreamReader(packetInputStream);
    this.decodingBuffer = new int[FlacFrameReader.TEMPORARY_BUFFER_SIZE];
    this.rawSampleBuffers = new int[info.stream.channelCount][];
    this.sampleBuffers = new short[info.stream.channelCount][];

    for (int i = 0; i < rawSampleBuffers.length; i++) {
      rawSampleBuffers[i] = new int[info.stream.maximumBlockSize];
      sampleBuffers[i] = new short[info.stream.maximumBlockSize];
    }
  }

  @Override
  public void initialise(AudioProcessingContext context) throws IOException {
    downstream = AudioPipelineFactory.create(context,
        new PcmFormat(info.stream.channelCount, info.stream.sampleRate));
  }

  @Override
  public OggMetadata getMetadata() {
    return new OggMetadata(info.tags);
  }

  @Override
  public OggStreamSizeInfo seekForSizeInfo() throws IOException {
    if (info.stream.sampleCount > 0) {
      return new OggStreamSizeInfo(0, info.stream.sampleCount, 0, 0, info.stream.sampleRate);
    } else {
      return packetInputStream.seekForSizeInfo(info.stream.sampleRate);
    }
  }

  @Override
  public void provideFrames() throws InterruptedException {
    try {
      while (packetInputStream.startNewPacket()) {
        int sampleCount = readFlacFrame();

        if (sampleCount == 0) {
          throw new IllegalStateException("Not enough bytes in packet.");
        }

        downstream.process(sampleBuffers, 0, sampleCount);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private int readFlacFrame() throws IOException {
    return FlacFrameReader.readFlacFrame(packetInputStream, bitStreamReader, info.stream, rawSampleBuffers, sampleBuffers, decodingBuffer);
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
  }
}
