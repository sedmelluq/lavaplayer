package com.sedmelluq.discord.lavaplayer.container.ogg;

public interface OggTrackBlueprint {
  OggTrackHandler loadTrackHandler(OggPacketInputStream stream);
}
