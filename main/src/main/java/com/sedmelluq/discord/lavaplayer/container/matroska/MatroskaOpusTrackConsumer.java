package com.sedmelluq.discord.lavaplayer.container.matroska;

import com.sedmelluq.discord.lavaplayer.filter.FilterChainBuilder;
import com.sedmelluq.discord.lavaplayer.filter.OpusEncodingPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.ShortPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.natives.opus.OpusDecoder;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.filter.volume.AudioFrameVolumeChanger;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;
import org.ebml.matroska.MatroskaFileFrame;
import org.ebml.matroska.MatroskaFileTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Consumes OPUS track data from a matroska file.
 */
public class MatroskaOpusTrackConsumer implements MatroskaTrackConsumer {
  private static final Logger log = LoggerFactory.getLogger(MatroskaOpusTrackConsumer.class);

  private final AudioProcessingContext context;
  private final MatroskaFileTrack track;
  private final boolean hasStandardInput;
  private final int inputFrequency;
  private final int inputChannels;

  private long currentTimecode;
  private boolean hasStandardSize;
  private OpusDecoder opusDecoder;
  private ShortPcmAudioFilter downstream;
  private ByteBuffer directInput;
  private ShortBuffer frameBuffer;

  /**
   * @param context Configuration and output information for processing
   * @param track The associated matroska track
   */
  public MatroskaOpusTrackConsumer(AudioProcessingContext context, MatroskaFileTrack track) {
    this.context = context;
    this.track = track;
    this.inputFrequency = (int) track.getAudio().getSamplingFrequency();
    this.inputChannels = track.getAudio().getChannels();
    this.hasStandardInput = inputFrequency == OpusEncodingPcmAudioFilter.FREQUENCY && inputChannels == OpusEncodingPcmAudioFilter.CHANNEL_COUNT;
    this.hasStandardSize = true;
    this.currentTimecode = 0;
  }

  @Override
  public MatroskaFileTrack getTrack() {
    return track;
  }

  @Override
  public void initialise() {
    // Nothing to do here
  }

  @Override
  public void seekPerformed(long requestedTimecode, long providedTimecode) {
    currentTimecode = providedTimecode;

    if (downstream != null) {
      downstream.seekPerformed(requestedTimecode, providedTimecode);
    }
  }

  @Override
  public void flush() throws InterruptedException {
    if (downstream != null) {
      downstream.flush();
    }
  }

  @Override
  public void consume(MatroskaFileFrame frame) throws InterruptedException {
    int frameSize = processFrameSize(frame.getData());

    if (frameSize != 0) {
      checkDecoderNecessity();

      if (opusDecoder != null) {
        passDownstream(frame.getData(), frameSize);
      } else {
        passThrough(frame.getData());
      }
    }
  }

  @Override
  public void close() {
    if (opusDecoder != null) {
      destroyDecoder();
    }
  }

  private int processFrameSize(ByteBuffer buffer) {
    int frameSize = OpusDecoder.getPacketFrameSize(inputFrequency, buffer.array(), buffer.position(), buffer.remaining());

    if (frameSize == 0) {
      return 0;
    } else if (frameSize != OpusEncodingPcmAudioFilter.FRAME_SIZE) {
      hasStandardSize = false;
    }

    currentTimecode += frameSize * 1000 / inputFrequency;
    return frameSize;
  }

  private void passDownstream(ByteBuffer buffer, int frameSize) throws InterruptedException {
    if (directInput == null || directInput.capacity() < buffer.remaining()) {
      directInput = ByteBuffer.allocateDirect(buffer.remaining() + 200);
    }

    directInput.clear();
    directInput.put(buffer);
    directInput.flip();

    if (frameBuffer == null || frameBuffer.capacity() < frameSize * inputChannels) {
      frameBuffer = ByteBuffer.allocateDirect(frameSize * inputChannels * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
    }

    frameBuffer.clear();
    frameBuffer.limit(frameSize);

    opusDecoder.decode(directInput, frameBuffer);
    downstream.process(frameBuffer);
  }

  private void passThrough(ByteBuffer buffer) throws InterruptedException {
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);

    context.frameConsumer.consume(new AudioFrame(currentTimecode, bytes, 100));
  }

  private boolean needsDecoding() {
    return context.volumeLevel.get() != 100 || !hasStandardSize || !hasStandardInput;
  }

  private void checkDecoderNecessity() {
    if (needsDecoding()) {
      if (opusDecoder == null) {
        log.debug("Enabling reencode mode on opus track.");

        initialiseDecoder();

        AudioFrameVolumeChanger.apply(context.configuration, context.frameConsumer, context.volumeLevel.get());
      }
    } else {
      if (opusDecoder != null) {
        log.debug("Enabling passthrough mode on opus track.");

        destroyDecoder();

        AudioFrameVolumeChanger.apply(context.configuration, context.frameConsumer, context.volumeLevel.get());
      }
    }
  }

  private void initialiseDecoder() {
    opusDecoder = new OpusDecoder(inputFrequency, inputChannels);
    downstream = FilterChainBuilder.forShortPcm(context, inputChannels, inputFrequency, true);
    downstream.seekPerformed(currentTimecode, currentTimecode);
  }

  private void destroyDecoder() {
    opusDecoder.close();
    opusDecoder = null;
    downstream.close();
    downstream = null;
    directInput = null;
    frameBuffer = null;
  }
}
