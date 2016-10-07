package com.sedmelluq.discord.lavaplayer.container.matroska;

import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAacTrackConsumer;
import com.sedmelluq.discord.lavaplayer.filter.FilterChainBuilder;
import com.sedmelluq.discord.lavaplayer.filter.ShortPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.natives.aac.AacDecoder;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameConsumer;
import org.ebml.matroska.MatroskaFileFrame;
import org.ebml.matroska.MatroskaFileTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consumes AAC track data from a matroska file.
 */
public class MatroskaAacTrackConsumer implements MatroskaTrackConsumer {
  private static final Logger log = LoggerFactory.getLogger(MpegAacTrackConsumer.class);

  private final MatroskaFileTrack track;
  private final ByteBuffer inputBuffer;
  private final ShortBuffer outputBuffer;
  private final ShortPcmAudioFilter downstream;

  private AacDecoder decoder;

  /**
   * @param manager Audio player manager which is used for configuration
   * @param track The MP4 audio track descriptor
   * @param frameConsumer Consumer of the decoded frames
   * @param volumeLevel Mutable volume level
   */
  public MatroskaAacTrackConsumer(AudioPlayerManager manager, AudioFrameConsumer frameConsumer, MatroskaFileTrack track, AtomicInteger volumeLevel) {
    this.track = track;
    this.decoder = new AacDecoder();
    this.inputBuffer = ByteBuffer.allocateDirect(4096);
    this.outputBuffer = ByteBuffer.allocateDirect(2048 * track.getAudio().getChannels()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
    this.downstream = FilterChainBuilder.forShortPcm(manager, frameConsumer, volumeLevel, track.getAudio().getChannels(),
        (int) track.getAudio().getSamplingFrequency(), true);
  }

  @Override
  public void initialise() {
    log.debug("Initialising AAC track with frequency {} and channel count {}.", track.getAudio().getSamplingFrequency(),
        track.getAudio().getChannels());

    configureDecoder();
  }

  private void configureDecoder() {
    ByteBuffer buffer = track.getCodecPrivate().duplicate();
    byte[] header = new byte[buffer.remaining()];
    buffer.get(header);
    decoder.configure(header);
  }

  @Override
  public MatroskaFileTrack getTrack() {
    return track;
  }

  @Override
  public void seekPerformed(long requestedTimecode, long providedTimecode) {
    downstream.seekPerformed(requestedTimecode, providedTimecode);

    decoder.close();
    decoder = new AacDecoder();
    configureDecoder();
  }

  @Override
  public void flush() throws InterruptedException {
    while (decoder.decode(outputBuffer, true)) {
      downstream.process(outputBuffer);
      outputBuffer.clear();
    }
  }

  @Override
  public void consume(MatroskaFileFrame frame) throws InterruptedException {
    ByteBuffer buffer = frame.getData();

    while (buffer.hasRemaining()) {
      int chunk = Math.min(buffer.remaining(), inputBuffer.capacity());
      ByteBuffer chunkBuffer = buffer.duplicate();
      chunkBuffer.limit(chunkBuffer.position() + chunk);

      inputBuffer.clear();
      inputBuffer.put(chunkBuffer);
      inputBuffer.flip();
      decoder.fill(inputBuffer);

      while (decoder.decode(outputBuffer, false)) {
        downstream.process(outputBuffer);
        outputBuffer.clear();
      }

      buffer.position(chunkBuffer.position());
    }
  }

  @Override
  public void close() {
    downstream.close();
    decoder.close();
  }
}
