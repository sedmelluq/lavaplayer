package com.sedmelluq.lavaplayer.core.container.playlists;

import com.sedmelluq.lavaplayer.core.container.adts.AdtsStreamPlayback;
import com.sedmelluq.lavaplayer.core.container.mpegts.MpegTsElementaryInputStream;
import com.sedmelluq.lavaplayer.core.container.mpegts.PesPacketInputStream;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import java.io.InputStream;

import static com.sedmelluq.lavaplayer.core.container.mpegts.MpegTsElementaryInputStream.ADTS_ELEMENTARY_STREAM;

public abstract class MpegTsM3uStreamPlayback extends M3uStreamPlayback {
  private final String identifier;

  protected MpegTsM3uStreamPlayback(String identifier) {
    this.identifier = identifier;
  }

  @Override
  protected void processJoinedStream(AudioPlaybackController controller, InputStream stream) {
    MpegTsElementaryInputStream elementaryInputStream = new MpegTsElementaryInputStream(stream, ADTS_ELEMENTARY_STREAM);
    PesPacketInputStream pesPacketInputStream = new PesPacketInputStream(elementaryInputStream);

    new AdtsStreamPlayback(identifier, pesPacketInputStream).process(controller);
  }
}
