package com.sedmelluq.discord.lavaplayer.natives.vorbis;

import com.sedmelluq.discord.lavaplayer.natives.ConnectorNativeLibLoader;

import java.nio.ByteBuffer;

class VorbisDecoderLibrary {
    private VorbisDecoderLibrary() {

    }

    static VorbisDecoderLibrary getInstance() {
        ConnectorNativeLibLoader.loadConnectorLibrary();
        return new VorbisDecoderLibrary();
    }

    native long create();

    native void destroy(long instance);

    native boolean initialise(long instance, ByteBuffer infoBuffer, int infoOffset, int infoLength,
                              ByteBuffer setupBuffer, int setupOffset, int setupLength);

    native int getChannelCount(long instance);

    native int input(long instance, ByteBuffer directBuffer, int offset, int length);

    native int output(long instance, float[][] channels, int length);
}
