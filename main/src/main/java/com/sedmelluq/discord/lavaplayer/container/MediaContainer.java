package com.sedmelluq.discord.lavaplayer.container;

import com.sedmelluq.discord.lavaplayer.container.matroska.MatroskaContainerProbe;
import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3ContainerProbe;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegContainerProbe;

/**
 * Lists currently supported containers and their probes.
 */
public enum MediaContainer {
  MKV(new MatroskaContainerProbe()),
  MP4(new MpegContainerProbe()),
  MP3(new Mp3ContainerProbe());

  /**
   * The probe used to detect files using this container and create the audio tracks for them.
   */
  public final MediaContainerProbe probe;

  MediaContainer(MediaContainerProbe probe) {
    this.probe = probe;
  }
}
