package com.sedmelluq.discord.lavaplayer.container.ogg;

import com.sedmelluq.discord.lavaplayer.tools.io.DirectBufferStreamBroker;

import java.io.IOException;

public interface OggCodecHandler {
    boolean isMatchingIdentifier(int identifier);

    int getMaximumFirstPacketLength();

    OggTrackBlueprint loadBlueprint(OggPacketInputStream stream, DirectBufferStreamBroker broker) throws IOException;

    OggMetadata loadMetadata(OggPacketInputStream stream, DirectBufferStreamBroker broker) throws IOException;
}
