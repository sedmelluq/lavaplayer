package com.sedmelluq.discord.lavaplayer.container.matroska;

import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameConsumer;
import org.ebml.matroska.MatroskaFileFrame;
import org.ebml.matroska.MatroskaFileTrack;

/**
 * Consumes OPUS track data from a matroska file.
 */
public class MatroskaOpusTrackConsumer implements MatroskaTrackConsumer {
  private AudioFrameConsumer frameConsumer;
  private MatroskaFileTrack track;

  /**
   * @param frameConsumer The consumer of the audio frames created from this track
   * @param track The associated matroska track
   */
  public MatroskaOpusTrackConsumer(AudioFrameConsumer frameConsumer, MatroskaFileTrack track) {
    this.frameConsumer = frameConsumer;
    this.track = track;
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
    // Nothing to do here
  }

  @Override
  public void flush() {
    // Nothing to do here
  }

  @Override
  public void consume(MatroskaFileFrame frame) throws InterruptedException {
    byte[] bytes = new byte[frame.getData().remaining()];
    frame.getData().get(bytes);

    frameConsumer.consume(new AudioFrame(frame.getTimecode(), bytes));
  }

  @Override
  public void close() {
    // Nothing to do here
  }
}
