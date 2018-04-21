package com.sedmelluq.discord.lavaplayer.container.matroska;

import com.sedmelluq.discord.lavaplayer.container.matroska.format.MatroskaFileTrack;
import com.sedmelluq.discord.lavaplayer.container.matroska.format.MatroskaFileTrack.AudioDetails;
import com.sedmelluq.discord.lavaplayer.filter.AudioPipeline;
import com.sedmelluq.discord.lavaplayer.filter.AudioPipelineFactory;
import com.sedmelluq.discord.lavaplayer.filter.PcmFormat;
import com.sedmelluq.discord.lavaplayer.natives.vorbis.VorbisDecoder;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.nio.ByteBuffer;

/**
 * Consumes Vorbis track data from a matroska file.
 */
public class MatroskaVorbisTrackConsumer implements MatroskaTrackConsumer {
  private static final int PCM_BUFFER_SIZE = 4096;
  private static final int COPY_BUFFER_SIZE = 256;

  private final MatroskaFileTrack track;
  private final VorbisDecoder decoder;
  private final byte[] copyBuffer;
  private final AudioPipeline downstream;
  private ByteBuffer inputBuffer;
  private float[][] channelPcmBuffers;

  /**
   * @param context Configuration and output information for processing
   * @param track The associated matroska track
   */
  public MatroskaVorbisTrackConsumer(AudioProcessingContext context, MatroskaFileTrack track) {

    this.track = track;
    this.decoder = new VorbisDecoder();
    this.copyBuffer = new byte[COPY_BUFFER_SIZE];

    AudioDetails audioTrack = fillMissingDetails(track.audio, track.codecPrivate);
    this.downstream = AudioPipelineFactory.create(context,
        new PcmFormat(audioTrack.channels, (int) audioTrack.samplingFrequency));
  }

  @Override
  public MatroskaFileTrack getTrack() {
    return track;
  }

  @Override
  public void initialise() {
    ByteBuffer directPrivateData = ByteBuffer.allocateDirect(track.codecPrivate.length);

    directPrivateData.put(track.codecPrivate);
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

  private static int readLacingValue(ByteBuffer buffer) {
    int value = 0;
    int current;

    do {
      current = buffer.get() & 0xFF;
      value += current;
    } while (current == 255);

    return value;
  }

  private static AudioDetails fillMissingDetails(AudioDetails details, byte[] headers) {
    if (details.channels != 0) {
      return details;
    }

    ByteBuffer buffer = ByteBuffer.wrap(headers);
    readLacingValue(buffer); // first header size
    readLacingValue(buffer); // second header size

    buffer.getInt(); // vorbis version
    int channelCount = buffer.get() & 0xFF;

    return new AudioDetails(details.samplingFrequency, details.outputSamplingFrequency, channelCount, details.bitDepth);
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
  public void consume(ByteBuffer data) throws InterruptedException {
    ByteBuffer directBuffer = getAsDirectBuffer(data);
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
