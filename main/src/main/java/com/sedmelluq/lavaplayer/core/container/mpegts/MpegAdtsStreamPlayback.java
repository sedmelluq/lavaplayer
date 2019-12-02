package com.sedmelluq.lavaplayer.core.container.mpegts;

import com.sedmelluq.lavaplayer.core.container.adts.AdtsStreamPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import java.io.InputStream;

public class MpegAdtsStreamPlayback implements AudioPlayback {
  private final String identifier;
  private final InputStream inputStream;

  public MpegAdtsStreamPlayback(String identifier, InputStream inputStream) {
    this.identifier = identifier;
    this.inputStream = inputStream;
  }

  @Override
  public void process(AudioPlaybackController controller) {
    MpegTsElementaryInputStream elementaryInputStream =
        new MpegTsElementaryInputStream(inputStream, MpegTsElementaryInputStream.ADTS_ELEMENTARY_STREAM);
    PesPacketInputStream pesPacketInputStream = new PesPacketInputStream(elementaryInputStream);

    new AdtsStreamPlayback(identifier, pesPacketInputStream).process(controller);
  }
}
