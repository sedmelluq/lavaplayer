package com.sedmelluq.discord.lavaplayer.natives.opus;

import com.sedmelluq.discord.lavaplayer.natives.ConnectorNativeLibLoader;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

class OpusEncoderLibrary {
  static final int APPLICATION_AUDIO = 2049;

  private OpusEncoderLibrary() {

  }

  static OpusEncoderLibrary getInstance() {
    ConnectorNativeLibLoader.loadConnectorLibrary();
    return new OpusEncoderLibrary();
  }

  native long create(int sampleRate, int channels, int application, int quality);

  native void destroy(long instance);

  native int encode(long instance, ShortBuffer directInput, int frameSize, ByteBuffer directOutput, int outputCapacity);
}
