package com.sedmelluq.discord.lavaplayer.filter;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Collection;

/**
 * Collects buffers of the required chunk size and passes them on to audio post processors.
 */
public class FinalPcmAudioFilter implements UniversalPcmAudioFilter {
  private static final Logger log = LoggerFactory.getLogger(FinalPcmAudioFilter.class);
  private static final short[] zeroPadding = new short[128];

  private final AudioDataFormat format;
  private final ShortBuffer frameBuffer;
  private final Collection<AudioPostProcessor> postProcessors;

  private long ignoredFrames;
  private long timecodeBase;
  private long timecodeSampleOffset;

  /**
   * @param context Configuration and output information for processing
   * @param postProcessors Post processors to pass the final audio buffers to
   */
  public FinalPcmAudioFilter(AudioProcessingContext context, Collection<AudioPostProcessor> postProcessors) {
    this.format = context.outputFormat;
    this.frameBuffer = ByteBuffer
        .allocateDirect(format.totalSampleCount() * 2)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer();
    this.postProcessors = postProcessors;

    timecodeBase = 0;
    timecodeSampleOffset = 0;
  }

  private short decodeSample(float sample) {
    return (short) Math.min(Math.max((int)(sample * 32768.f), -32768), 32767);
  }

  @Override
  public void seekPerformed(long requestedTime, long providedTime) {
    frameBuffer.clear();
    ignoredFrames = requestedTime > providedTime ? (requestedTime - providedTime) * format.channelCount * format.sampleRate / 1000L : 0;
    timecodeBase = Math.max(requestedTime, providedTime);
    timecodeSampleOffset = 0;

    if (ignoredFrames > 0) {
      log.debug("Ignoring {} frames due to inaccurate seek (requested {}, provided {}).", ignoredFrames, requestedTime, providedTime);
    }
  }

  @Override
  public void flush() throws InterruptedException {
    if (frameBuffer.position() > 0) {
      fillFrameBuffer();
      dispatch();
    }
  }

  @Override
  public void close() {
    for (AudioPostProcessor postProcessor : postProcessors) {
      postProcessor.close();
    }
  }

  private void fillFrameBuffer() {
    while (frameBuffer.remaining() >= zeroPadding.length) {
      frameBuffer.put(zeroPadding);
    }

    while (frameBuffer.remaining() > 0) {
      frameBuffer.put((short) 0);
    }
  }

  @Override
  public void process(short[] input, int offset, int length) throws InterruptedException {
    for (int i = 0; i < length; i++) {
      if (ignoredFrames > 0) {
        ignoredFrames--;
      } else {
        frameBuffer.put(input[offset + i]);

        dispatch();
      }
    }
  }

  @Override
  public void process(short[][] input, int offset, int length) throws InterruptedException {
    int secondChannelIndex = Math.min(1, input.length - 1);

    for (int i = 0; i < length; i++) {
      if (ignoredFrames > 0) {
        ignoredFrames -= format.channelCount;
      } else {
        frameBuffer.put(input[0][offset + i]);
        frameBuffer.put(input[secondChannelIndex][offset + i]);

        dispatch();
      }
    }
  }

  @Override
  public void process(ShortBuffer buffer) throws InterruptedException {
    if (ignoredFrames > 0) {
      long skipped = Math.min(buffer.remaining(), ignoredFrames);
      buffer.position(buffer.position() + (int) skipped);
      ignoredFrames -= skipped;
    }

    ShortBuffer local = buffer.duplicate();

    while (buffer.remaining() > 0) {
      int chunk = Math.min(buffer.remaining(), frameBuffer.remaining());
      local.position(buffer.position());
      local.limit(local.position() + chunk);

      frameBuffer.put(local);
      dispatch();

      buffer.position(buffer.position() + chunk);
    }
  }

  @Override
  public void process(float[][] buffer, int offset, int length) throws InterruptedException {
    int secondChannelIndex = Math.min(1, buffer.length - 1);

    for (int i = 0; i < length; i++) {
      if (ignoredFrames > 0) {
        ignoredFrames -= 2;
      } else {
        frameBuffer.put(decodeSample(buffer[0][offset + i]));
        frameBuffer.put(decodeSample(buffer[secondChannelIndex][offset + i]));

        dispatch();
      }
    }
  }

  private void dispatch() throws InterruptedException {
    if (!frameBuffer.hasRemaining()) {
      long timecode = timecodeBase + timecodeSampleOffset * 1000 / format.sampleRate;
      frameBuffer.clear();

      for (AudioPostProcessor postProcessor : postProcessors) {
        postProcessor.process(timecode, frameBuffer);
      }

      frameBuffer.clear();

      timecodeSampleOffset += format.chunkSampleCount;
    }
  }
}
