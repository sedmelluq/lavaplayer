package com.sedmelluq.lavaplayer.extensions.format.xm;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerProbe;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import ibxm.IBXM;
import ibxm.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.UNKNOWN_ARTIST;
import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult.supportedFormat;

public class ModContainerProbe implements MediaContainerProbe {
  private static final Logger log = LoggerFactory.getLogger(ModContainerProbe.class);

  @Override
  public String getName() {
    return "mod";
  }

  @Override
  public boolean matchesHints(MediaContainerHints hints) {
    return false;
  }

  @Override
  public MediaContainerDetectionResult probe(AudioReference reference, SeekableInputStream inputStream) throws IOException {
    Module module;
    try {
      module = new Module(inputStream);
    } catch (IllegalArgumentException e) {
      return null;
    }

    log.debug("Track {} is a module.", reference.identifier);

    inputStream.seek(0);

    return supportedFormat(this, null, new AudioTrackInfo(
        module.songName,
        UNKNOWN_ARTIST,
        getLength(module),
        reference.identifier,
        true,
        reference.identifier
    ));
  }

  public int getLength(Module module) {
    IBXM ibxm = new IBXM(module, 48000);
    return (ibxm.calculateSongDuration() / 48000) * 1000;
  }

  @Override
  public AudioTrack createTrack(String parameters, AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    return new XmAudioTrack(trackInfo, inputStream);
  }

}
