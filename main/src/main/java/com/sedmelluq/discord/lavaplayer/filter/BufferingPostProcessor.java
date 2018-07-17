package com.sedmelluq.discord.lavaplayer.filter;

import com.sedmelluq.discord.lavaplayer.format.transcoder.AudioChunkEncoder;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * Post processor which encodes audio chunks and passes them as audio frames to the frame buffer.
 */
public class BufferingPostProcessor implements AudioPostProcessor {
  private final AudioProcessingContext context;
  private final AudioChunkEncoder encoder;
  private final MutableAudioFrame offeredFrame;
  private final ByteBuffer outputBuffer;

  /**
   * @param context Processing context to determine the destination buffer from.
   * @param encoder Encoder to encode the chunk with.
   */
  public BufferingPostProcessor(AudioProcessingContext context, AudioChunkEncoder encoder) {
    this.encoder = encoder;
    this.context = context;
    this.offeredFrame = new MutableAudioFrame();
    this.outputBuffer = ByteBuffer.allocateDirect(context.outputFormat.maximumChunkSize());

    offeredFrame.setFormat(context.outputFormat);
  }

  @Override
  public void process(long timecode, ShortBuffer buffer) throws InterruptedException {
    outputBuffer.clear();
    encoder.encode(buffer, outputBuffer);

    offeredFrame.setTimecode(timecode);
    offeredFrame.setVolume(context.playerOptions.volumeLevel.get());

    offeredFrame.setBuffer(outputBuffer);
    context.frameBuffer.consume(offeredFrame);
  }

  @Override
  public void close() {
    encoder.close();
  }
}
