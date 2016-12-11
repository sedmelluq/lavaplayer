package com.sedmelluq.discord.lavaplayer.natives.samplerate;

import com.sedmelluq.discord.lavaplayer.natives.ConnectorNativeLibLoader;

class SampleRateLibrary {
  private SampleRateLibrary() {

  }

  static SampleRateLibrary getInstance() {
    ConnectorNativeLibLoader.loadConnectorLibrary();
    return new SampleRateLibrary();
  }

  native long create(int type, int channels);

  native void destroy(long instance);

  native void reset(long instance);

  native int process(long instance, float[] in, int inOffset, int inLength, float[] out, int outOffset, int outLength, boolean endOfInput, double sourceRatio, int[] progress);
}
