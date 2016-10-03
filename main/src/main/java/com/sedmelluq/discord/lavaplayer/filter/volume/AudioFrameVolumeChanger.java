package com.sedmelluq.discord.lavaplayer.filter.volume;

import com.sedmelluq.discord.lavaplayer.natives.opus.OpusDecoder;
import com.sedmelluq.discord.lavaplayer.natives.opus.OpusEncoder;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameConsumer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameRebuilder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import static com.sedmelluq.discord.lavaplayer.filter.OpusEncodingPcmAudioFilter.CHANNEL_COUNT;
import static com.sedmelluq.discord.lavaplayer.filter.OpusEncodingPcmAudioFilter.FRAME_SIZE;
import static com.sedmelluq.discord.lavaplayer.filter.OpusEncodingPcmAudioFilter.FREQUENCY;

/**
 * A frame rebuilder to apply a specific volume level to the frames.
 */
public class AudioFrameVolumeChanger implements AudioFrameRebuilder {
  private final int newVolume;
  private final ByteBuffer encodedBuffer;
  private final ShortBuffer sampleBuffer;
  private final PcmVolumeProcessor volumeProcessor;

  private OpusEncoder encoder;
  private OpusDecoder decoder;

  private AudioFrameVolumeChanger(int newVolume) {
    this.newVolume = newVolume;

    this.encodedBuffer = ByteBuffer.allocateDirect(4096);
    this.sampleBuffer = ByteBuffer.allocateDirect(FRAME_SIZE * CHANNEL_COUNT * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
    this.volumeProcessor = new PcmVolumeProcessor(100);
  }

  @Override
  public AudioFrame rebuild(AudioFrame frame) {
    if (frame.volume == newVolume) {
      return frame;
    }

    encodedBuffer.clear();
    encodedBuffer.put(frame.data);
    encodedBuffer.flip();

    sampleBuffer.clear();
    decoder.decode(encodedBuffer, sampleBuffer);

    encodedBuffer.clear();

    volumeProcessor.applyVolume(frame.volume, newVolume, sampleBuffer);

    encoder.encode(sampleBuffer, FRAME_SIZE, encodedBuffer);

    byte[] bytes = new byte[encodedBuffer.remaining()];
    encodedBuffer.get(bytes);

    return new AudioFrame(frame.timecode, bytes, newVolume);
  }

  private void setupLibraries() {
    encoder = new OpusEncoder(FREQUENCY, CHANNEL_COUNT);
    decoder = new OpusDecoder(FREQUENCY, CHANNEL_COUNT);
  }

  private void clearLibraries() {
    if (encoder != null) {
      encoder.close();
    }

    if (decoder != null) {
      decoder.close();
    }
  }

  /**
   * Applies a volume level to the buffered frames of a frame consumer
   * @param frameConsumer The frame consumer
   * @param newVolume New volume to apply
   */
  public static void apply(AudioFrameConsumer frameConsumer, int newVolume) {
    AudioFrameVolumeChanger volumeChanger = new AudioFrameVolumeChanger(newVolume);

    try {
      volumeChanger.setupLibraries();
      frameConsumer.rebuild(volumeChanger);
    } finally {
      volumeChanger.clearLibraries();
    }
  }
}
