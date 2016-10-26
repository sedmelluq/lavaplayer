package com.sedmelluq.discord.lavaplayer.container.matroska;

import com.sedmelluq.discord.lavaplayer.container.common.OpusPacketRouter;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;
import org.ebml.matroska.MatroskaFileFrame;
import org.ebml.matroska.MatroskaFileTrack;

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
    this.opusPacketRouter = new OpusPacketRouter(context, (int) track.getAudio().getSamplingFrequency(), track.getAudio().getChannels());
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
  public void consume(MatroskaFileFrame frame) throws InterruptedException {
    opusPacketRouter.process(frame.getData());
  }

  @Override
  public void close() {
    opusPacketRouter.close();
  }
}
