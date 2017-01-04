package com.sedmelluq.discord.lavaplayer.container.matroska;

import com.sedmelluq.discord.lavaplayer.container.common.OpusPacketRouter;
import com.sedmelluq.discord.lavaplayer.container.matroska.format.MatroskaFileTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.nio.ByteBuffer;

/**
 * Consumes OPUS track data from a matroska file.
 */
public class MatroskaOpusTrackConsumer implements MatroskaTrackConsumer {
  private final MatroskaFileTrack track;
  private final OpusPacketRouter opusPacketRouter;

  /**
   * @param context Configuration and output information for processing
   * @param track The associated matroska track
   */
  public MatroskaOpusTrackConsumer(AudioProcessingContext context, MatroskaFileTrack track) {
    this.track = track;
    this.opusPacketRouter = new OpusPacketRouter(context, (int) track.audio.samplingFrequency, track.audio.channels);
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
    opusPacketRouter.seekPerformed(requestedTimecode, providedTimecode);
  }

  @Override
  public void flush() throws InterruptedException {
    opusPacketRouter.flush();
  }

  @Override
  public void consume(ByteBuffer data) throws InterruptedException {
    opusPacketRouter.process(data);
  }

  @Override
  public void close() {
    opusPacketRouter.close();
  }
}
