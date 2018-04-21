package com.sedmelluq.discord.lavaplayer.container.mpeg;

import com.sedmelluq.discord.lavaplayer.filter.AudioPipeline;
import com.sedmelluq.discord.lavaplayer.filter.AudioPipelineFactory;
import com.sedmelluq.discord.lavaplayer.filter.PcmFormat;
import com.sedmelluq.discord.lavaplayer.natives.aac.AacDecoder;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ReadableByteChannel;

/**
 * Handles processing MP4 AAC frames. Passes the decoded frames to the specified frame consumer. Currently only AAC LC
 * format is supported, although the underlying decoder can handler other types as well.
 */
public class MpegAacTrackConsumer implements MpegTrackConsumer {
  private static final Logger log = LoggerFactory.getLogger(MpegAacTrackConsumer.class);

  private final MpegTrackInfo track;
  private final ByteBuffer inputBuffer;
  private final ShortBuffer outputBuffer;
  private final AudioPipeline downstream;

  private AacDecoder decoder;

  /**
   * @param context Configuration and output information for processing
   * @param track The MP4 audio track descriptor
   */
  public MpegAacTrackConsumer(AudioProcessingContext context, MpegTrackInfo track) {
    this.track = track;
    this.decoder = new AacDecoder();
    this.inputBuffer = ByteBuffer.allocateDirect(4096);
    this.outputBuffer = ByteBuffer.allocateDirect(2048 * track.channelCount).order(ByteOrder.nativeOrder()).asShortBuffer();
    this.downstream = AudioPipelineFactory.create(context, new PcmFormat(track.channelCount, track.sampleRate));
  }

  @Override
  public void initialise() {
    log.debug("Initialising AAC track with frequency {} and channel count {}.", track.sampleRate, track.channelCount);

    decoder.configure(AacDecoder.AAC_LC, track.sampleRate, track.channelCount);
  }

  @Override
  public MpegTrackInfo getTrack() {
    return track;
  }

  @Override
  public void seekPerformed(long requestedTimecode, long providedTimecode) {
    downstream.seekPerformed(requestedTimecode, providedTimecode);

    decoder.close();
    decoder = new AacDecoder();
    decoder.configure(AacDecoder.AAC_LC, track.sampleRate, track.channelCount);
  }

  @Override
  public void flush() throws InterruptedException {
    while (decoder.decode(outputBuffer, true)) {
      downstream.process(outputBuffer);
      outputBuffer.clear();
    }
  }

  @Override
  public void consume(ReadableByteChannel channel, int length) throws InterruptedException {
    int remaining = length;

    while (remaining > 0) {
      int chunk = Math.min(remaining, inputBuffer.capacity());

      inputBuffer.clear();
      inputBuffer.limit(chunk);

      try {
        IOUtils.readFully(channel, inputBuffer);
      } catch (ClosedByInterruptException e) {
        log.trace("Interrupt received while reading channel", e);

        Thread.currentThread().interrupt();
        throw new InterruptedException();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      inputBuffer.flip();
      decoder.fill(inputBuffer);

      while (decoder.decode(outputBuffer, false)) {
        downstream.process(outputBuffer);
        outputBuffer.clear();
      }

      remaining -= chunk;
    }
  }

  @Override
  public void close() {
    downstream.close();
    decoder.close();
  }
}
