package com.sedmelluq.discord.lavaplayer.natives.aac;

import com.sedmelluq.discord.lavaplayer.natives.ConnectorNativeLibLoader;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

class AacDecoderLibrary {
  private AacDecoderLibrary() {

  }

  static AacDecoderLibrary getInstance() {
    ConnectorNativeLibLoader.loadConnectorLibrary();
    return new AacDecoderLibrary();
  }

  native long create(int transportType);

  native void destroy(long instance);

  native int configure(long instance, long bufferData);

  native int fill(long instance, ByteBuffer directBuffer, int offset, int length);

  native int decode(long instance, ShortBuffer directBuffer, int length, boolean flush);

  native long getStreamInfo(long instance);
}
