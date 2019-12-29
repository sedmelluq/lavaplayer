package com.sedmelluq.lavaplayer.extensions.format.xm;

import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;
import ibxm.Channel;
import ibxm.IBXM;
import ibxm.Module;

import java.io.IOException;

public class XmFileLoader {
  private final SeekableInputStream inputStream;

  public XmFileLoader(SeekableInputStream inputStream) {
    this.inputStream = inputStream;
  }

  public XmTrackProvider loadTrack(AudioProcessingContext context) throws IOException {
    Module module = new Module(inputStream);
    IBXM ibxm = new IBXM(module, context.outputFormat.sampleRate);
    ibxm.setInterpolation(Channel.SINC);
    return new XmTrackProvider(context, ibxm);
  }
}
