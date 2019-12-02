package com.sedmelluq.lavaplayer.core.container.ogg;

public interface OggTrackBlueprint {
  OggTrackHandler loadTrackHandler(OggPacketInputStream stream);
}
