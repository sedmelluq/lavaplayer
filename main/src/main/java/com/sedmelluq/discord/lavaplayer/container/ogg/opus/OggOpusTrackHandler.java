package com.sedmelluq.discord.lavaplayer.container.ogg.opus;

import com.sedmelluq.discord.lavaplayer.container.common.OpusPacketRouter;
import com.sedmelluq.discord.lavaplayer.container.ogg.OggPacketInputStream;
import com.sedmelluq.discord.lavaplayer.container.ogg.OggTrackHandler;
import com.sedmelluq.discord.lavaplayer.tools.io.DirectBufferStreamBroker;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * OGG stream handler for Opus codec.
 */
public class OggOpusTrackHandler implements OggTrackHandler {
  private final OggPacketInputStream packetInputStream;
  private final DirectBufferStreamBroker broker;
  private final int channelCount;
  private final int sampleRate;
  private OpusPacketRouter opusPacketRouter;

  /**
   * @param packetInputStream OGG packet input stream
   * @param broker Broker for loading stream data into direct byte buffer.
   * @param channelCount Number of channels in the track.
   * @param sampleRate Sample rate of the track.
   */
  public OggOpusTrackHandler(OggPacketInputStream packetInputStream, DirectBufferStreamBroker broker, int channelCount,
                             int sampleRate) {

    this.packetInputStream = packetInputStream;
    this.broker = broker;
    this.channelCount = channelCount;
    this.sampleRate = sampleRate;
  }

  @Override
  public void initialise(AudioProcessingContext context, long timecode, long desiredTimecode) {
    opusPacketRouter = new OpusPacketRouter(context, sampleRate, channelCount);
    opusPacketRouter.seekPerformed(desiredTimecode, timecode);
  }

  @Override
  public void provideFrames() throws InterruptedException {
    try {
      while (packetInputStream.startNewPacket()) {
        broker.consumeNext(packetInputStream, Integer.MAX_VALUE, Integer.MAX_VALUE);

        ByteBuffer buffer = broker.getBuffer();

        if (buffer.remaining() > 0) {
          opusPacketRouter.process(buffer);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void seekToTimecode(long timecode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    if (opusPacketRouter != null) {
      opusPacketRouter.close();
    }
  }
}
