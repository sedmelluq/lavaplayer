package com.sedmelluq.discord.lavaplayer.natives.samplerate;

import com.sedmelluq.discord.lavaplayer.natives.NativeLibLoader;

class SampleRateLibrary {
  private SampleRateLibrary() {

  }

  static SampleRateLibrary getInstance() {
    NativeLibLoader.load("connector");
    return new SampleRateLibrary();
  }

  native long create(int type, int channels);

  native void destroy(long instance);

  native void reset(long instance);

  native int process(long instance, float[] in, int inOffset, int inLength, float[] out, int outOffset, int outLength, boolean endOfInput, double sourceRatio, int[] progress);

  enum Type {
    SINC_BEST_QUALITY,
    SINC_MEDIUM_QUALITY,
    SINC_FASTEST,
    ZERO_ORDER_HOLD,
    LINEAR
  }
}
