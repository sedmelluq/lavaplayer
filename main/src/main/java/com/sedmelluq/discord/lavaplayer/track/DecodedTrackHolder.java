package com.sedmelluq.discord.lavaplayer.track;

/**
 * The result of decoding a track.
 */
public class DecodedTrackHolder {
  /**
   * The decoded track. This may be null if there was a track to decode, but the decoding could not be performed because
   * of an older serialization version or because the track source it used is not loaded.
   */
  public final AudioTrack decodedTrack;

  /**
   * @param decodedTrack The decoded track
   */
  public DecodedTrackHolder(AudioTrack decodedTrack) {
    this.decodedTrack = decodedTrack;
  }
}
