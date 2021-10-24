package com.sedmelluq.discord.lavaplayer.container.ogg;

import com.sedmelluq.discord.lavaplayer.container.ogg.flac.OggFlacCodecHandler;
import com.sedmelluq.discord.lavaplayer.container.ogg.opus.OggOpusCodecHandler;
import com.sedmelluq.discord.lavaplayer.container.ogg.vorbis.OggVorbisCodecHandler;
import com.sedmelluq.discord.lavaplayer.tools.io.DirectBufferStreamBroker;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * Track loader for an OGG packet stream. Automatically detects the track codec and loads the specific track handler.
 */
public class OggTrackLoader {
    private static final OggCodecHandler[] TRACK_PROVIDERS = new OggCodecHandler[]{
        new OggOpusCodecHandler(),
        new OggFlacCodecHandler(),
        new OggVorbisCodecHandler()
    };

    private static final int MAXIMUM_FIRST_PACKET_LENGTH = Stream.of(TRACK_PROVIDERS)
        .mapToInt(OggCodecHandler::getMaximumFirstPacketLength).max().getAsInt();

    /**
     * @param packetInputStream OGG packet input stream
     * @return The track handler detected from this packet input stream. Returns null if the stream ended.
     * @throws IOException           On read error
     * @throws IllegalStateException If the track uses an unknown codec.
     */
    public static OggTrackBlueprint loadTrackBlueprint(OggPacketInputStream packetInputStream) throws IOException {
        CodecDetection result = detectCodec(packetInputStream);
        return result != null ? result.provider.loadBlueprint(packetInputStream, result.broker) : null;
    }

    public static OggMetadata loadMetadata(OggPacketInputStream packetInputStream) throws IOException {
        CodecDetection result = detectCodec(packetInputStream);
        return result != null ? result.provider.loadMetadata(packetInputStream, result.broker) : null;
    }

    private static CodecDetection detectCodec(OggPacketInputStream stream) throws IOException {
        if (!stream.startNewTrack() || !stream.startNewPacket()) {
            return null;
        }

        DirectBufferStreamBroker broker = new DirectBufferStreamBroker(1024);
        int maximumLength = MAXIMUM_FIRST_PACKET_LENGTH + 1;

        if (!broker.consumeNext(stream, maximumLength, maximumLength)) {
            throw new IOException("First packet is too large for any known OGG codec.");
        }

        int headerIdentifier = broker.getBuffer().getInt();

        for (OggCodecHandler trackProvider : TRACK_PROVIDERS) {
            if (trackProvider.isMatchingIdentifier(headerIdentifier)) {
                return new CodecDetection(trackProvider, broker);
            }
        }

        throw new IllegalStateException("Unsupported track in OGG stream.");
    }

    private static class CodecDetection {
        private final OggCodecHandler provider;
        private final DirectBufferStreamBroker broker;

        private CodecDetection(OggCodecHandler provider, DirectBufferStreamBroker broker) {
            this.provider = provider;
            this.broker = broker;
        }
    }
}
