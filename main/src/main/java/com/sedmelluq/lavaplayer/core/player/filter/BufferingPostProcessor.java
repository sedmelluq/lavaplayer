package com.sedmelluq.lavaplayer.core.player.filter;

import com.sedmelluq.lavaplayer.core.format.transcoder.AudioChunkEncoder;

import com.sedmelluq.lavaplayer.core.format.AudioDataFormat;
import com.sedmelluq.lavaplayer.core.player.frame.MutableAudioFrame;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackContext;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * Post processor which encodes audio chunks and passes them as audio frames to the frame buffer.
 */
public class BufferingPostProcessor implements AudioPostProcessor {
  private final AudioPlaybackContext context;
  private final AudioChunkEncoder encoder;
  private final MutableAudioFrame offeredFrame;
  private final ByteBuffer outputBuffer;

  /**
   * @param context Processing context to determine the destination buffer from.
   * @param encoder Encoder to encode the chunk with.
   */
  public BufferingPostProcessor(AudioPlaybackContext context, AudioChunkEncoder encoder) {
    this.encoder = encoder;
    this.context = context;
    this.offeredFrame = new MutableAudioFrame();

    AudioDataFormat format = context.getConfiguration().getOutputFormat();
    this.outputBuffer = ByteBuffer.allocateDirect(format.maximumChunkSize());

    offeredFrame.setFormat(format);
  }

  @Override
  public void process(long timecode, ShortBuffer buffer) throws InterruptedException {
    outputBuffer.clear();
    encoder.encode(buffer, outputBuffer);

    offeredFrame.setTimecode(timecode);
    offeredFrame.setVolume(context.getConfiguration().getVolumeLevel());

    offeredFrame.setBuffer(outputBuffer);
    context.getFrameBuffer().consume(offeredFrame);
  }

  @Override
  public void close() {
    encoder.close();
  }
}
