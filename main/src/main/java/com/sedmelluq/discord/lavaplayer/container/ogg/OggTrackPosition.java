package com.sedmelluq.discord.lavaplayer.container.ogg;

public class OggTrackPosition {
  public static final OggTrackPosition ZERO = new OggTrackPosition(0, 0);

  public final long desiredPosition;
  public final long actualPosition;

  public OggTrackPosition(long desiredPosition, long actualPosition) {
    this.desiredPosition = desiredPosition;
    this.actualPosition = actualPosition;
  }
}
