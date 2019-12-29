package com.sedmelluq.lavaplayer.extensions.format.xm;

import com.sedmelluq.discord.lavaplayer.filter.AudioPipeline;
import com.sedmelluq.discord.lavaplayer.filter.AudioPipelineFactory;
import com.sedmelluq.discord.lavaplayer.filter.PcmFormat;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;
import ibxm.IBXM;

public class XmTrackProvider {
  private final IBXM ibxm;
  private final AudioPipeline downstream;
  private final int blocksInBuffer;

  public XmTrackProvider(AudioProcessingContext context, IBXM ibxm) {
    this.ibxm = ibxm;
    this.downstream = AudioPipelineFactory.create(context, new PcmFormat(2, ibxm.getSampleRate()));
    this.blocksInBuffer = ibxm.getMixBufferLength();
  }

  public void provideFrames() throws InterruptedException {
    int blockCount;
    int[] buffer = new int[blocksInBuffer];
    short[] shortBuffer = new short[blocksInBuffer];

    while ((blockCount = ibxm.getAudio(buffer)) > 0) {
      for (int i = 0; i < blocksInBuffer; i++) {
        shortBuffer[i] = (short) buffer[i];
      }

      downstream.process(shortBuffer, 0, blockCount * 2);
    }
  }

  public void close() {
    downstream.close();
  }
}
