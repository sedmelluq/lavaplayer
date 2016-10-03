package com.sedmelluq.discord.lavaplayer.container.matroska;

import com.sedmelluq.discord.lavaplayer.filter.FilterChainBuilder;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameConsumer;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.natives.vorbis.VorbisDecoder;
import org.ebml.matroska.MatroskaFileFrame;
import org.ebml.matroska.MatroskaFileTrack;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consumes Vorbis track data from a matroska file.
 */
public class MatroskaVorbisTrackConsumer implements MatroskaTrackConsumer {
  private static final int PCM_BUFFER_SIZE = 4096;
  private static final int COPY_BUFFER_SIZE = 256;

  private final MatroskaFileTrack track;
  private final VorbisDecoder decoder;
  private final byte[] copyBuffer;
  private final FloatPcmAudioFilter downstream;
  private ByteBuffer inputBuffer;
  private float[][] channelPcmBuffers;

  /**
   * @param frameConsumer The consumer of the audio frames created from this track
   * @param track The associated matroska track
   * @param volumeLevel Mutable volume level
   */
  public MatroskaVorbisTrackConsumer(AudioFrameConsumer frameConsumer, MatroskaFileTrack track, AtomicInteger volumeLevel) {
    this.track = track;
    this.decoder = new VorbisDecoder();
    this.copyBuffer = new byte[COPY_BUFFER_SIZE];

    MatroskaFileTrack.MatroskaAudioTrack audioTrack = track.getAudio();
    this.downstream = FilterChainBuilder.forFloatPcm(frameConsumer, volumeLevel, audioTrack.getChannels(), (int) audioTrack.getSamplingFrequency());
  }

  @Override
  public MatroskaFileTrack getTrack() {
    return track;
  }

  @Override
  public void initialise() {
    ByteBuffer privateData = track.getCodecPrivate();
    ByteBuffer directPrivateData = ByteBuffer.allocateDirect(privateData.remaining());

    directPrivateData.put(privateData.array(), privateData.arrayOffset() + privateData.position(), privateData.remaining());
    directPrivateData.flip();

    try {
      int lengthInfoSize = directPrivateData.get();

      if (lengthInfoSize != 2) {
        throw new IllegalStateException("Unexpected lacing count.");
      }

      int firstHeaderSize = readLacingValue(directPrivateData);
      int secondHeaderSize = readLacingValue(directPrivateData);

      decoder.parseHeader(directPrivateData, firstHeaderSize, true);
      decoder.parseHeader(directPrivateData, secondHeaderSize, false);
      decoder.parseHeader(directPrivateData, directPrivateData.remaining(), false);
      decoder.initialise();

      channelPcmBuffers = new float[decoder.getChannelCount()][];

      for (int i = 0; i < channelPcmBuffers.length; i++) {
        channelPcmBuffers[i] = new float[PCM_BUFFER_SIZE];
      }
    } catch (Exception e) {
      throw new RuntimeException("Reading Vorbis header failed.", e);
    }
  }

  private int readLacingValue(ByteBuffer buffer) {
    int value = 0;
    int current;

    do {
      current = buffer.get() & 0xFF;
      value += current;
    } while (current == 255);

    return value;
  }

  @Override
  public void seekPerformed(long requestedTimecode, long providedTimecode) {
    downstream.seekPerformed(requestedTimecode, providedTimecode);
  }

  @Override
  public void flush() throws InterruptedException {
    downstream.flush();
  }

  private ByteBuffer getDirectBuffer(int size) {
    if (inputBuffer == null || inputBuffer.capacity() < size) {
      inputBuffer = ByteBuffer.allocateDirect(size * 3 / 2);
    }

    inputBuffer.clear();
    return inputBuffer;
  }

  private ByteBuffer getAsDirectBuffer(ByteBuffer data) {
    ByteBuffer buffer = getDirectBuffer(data.remaining());

    while (data.remaining() > 0) {
      int chunk = Math.min(copyBuffer.length, data.remaining());
      data.get(copyBuffer, 0, chunk);
      buffer.put(copyBuffer, 0, chunk);
    }

    buffer.flip();
    return buffer;
  }

  @Override
  public void consume(MatroskaFileFrame frame) throws InterruptedException {
    ByteBuffer directBuffer = getAsDirectBuffer(frame.getData());
    decoder.input(directBuffer);

    int output;

    do {
      output = decoder.output(channelPcmBuffers);

      if (output > 0) {
        downstream.process(channelPcmBuffers, 0, output);
      }
    } while (output == PCM_BUFFER_SIZE);
  }

  @Override
  public void close() {
    downstream.close();
    decoder.close();
  }
}
