package com.sedmelluq.discord.lavaplayer.natives.mp3;

import com.sedmelluq.discord.lavaplayer.natives.NativeLibLoader;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

class Mp3DecoderLibrary {
  private Mp3DecoderLibrary() {

  }

  static Mp3DecoderLibrary getInstance() {
    NativeLibLoader.loadConnectorLibrary();
    return new Mp3DecoderLibrary();
  }

  native long create();

  native void destroy(long instance);

  native int decode(long instance, ByteBuffer directInput, int inputLength, ShortBuffer directOutput, int outputLengthInBytes);
}
