package com.sedmelluq.lavaplayer.core.container;

import com.sedmelluq.lavaplayer.core.container.adts.AdtsContainerProbe;
import com.sedmelluq.lavaplayer.core.container.flac.FlacContainerProbe;
import com.sedmelluq.lavaplayer.core.container.matroska.MatroskaContainerProbe;
import com.sedmelluq.lavaplayer.core.container.mp3.Mp3ContainerProbe;
import com.sedmelluq.lavaplayer.core.container.mpeg.MpegContainerProbe;
import com.sedmelluq.lavaplayer.core.container.mpegts.MpegAdtsContainerProbe;
import com.sedmelluq.lavaplayer.core.container.ogg.OggContainerProbe;
import com.sedmelluq.lavaplayer.core.container.playlists.M3uPlaylistContainerProbe;
import com.sedmelluq.lavaplayer.core.container.playlists.PlainPlaylistContainerProbe;
import com.sedmelluq.lavaplayer.core.container.playlists.PlsPlaylistContainerProbe;
import com.sedmelluq.lavaplayer.core.container.wav.WavContainerProbe;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists currently supported containers and their probes.
 */
public enum MediaContainer {
  WAV(new WavContainerProbe()),
  MKV(new MatroskaContainerProbe()),
  MP4(new MpegContainerProbe()),
  FLAC(new FlacContainerProbe()),
  OGG(new OggContainerProbe()),
  M3U(new M3uPlaylistContainerProbe()),
  PLS(new PlsPlaylistContainerProbe()),
  PLAIN(new PlainPlaylistContainerProbe()),
  MP3(new Mp3ContainerProbe()),
  ADTS(new AdtsContainerProbe()),
  MPEGADTS(new MpegAdtsContainerProbe());

  /**
   * The probe used to detect files using this container and create the audio tracks for them.
   */
  public final MediaContainerProbe probe;

  MediaContainer(MediaContainerProbe probe) {
    this.probe = probe;
  }

  public static List<MediaContainerProbe> asList() {
    List<MediaContainerProbe> probes = new ArrayList<>();

    for (MediaContainer container : MediaContainer.class.getEnumConstants()) {
      probes.add(container.probe);
    }

    return probes;
  }
}
