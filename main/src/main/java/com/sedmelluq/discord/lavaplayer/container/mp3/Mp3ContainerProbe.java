package com.sedmelluq.discord.lavaplayer.container.mp3;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerProbe;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.checkNextBytes;

/**
 * Container detection probe for MP3 format.
 */
public class Mp3ContainerProbe implements MediaContainerProbe {
  private static final Logger log = LoggerFactory.getLogger(Mp3ContainerProbe.class);

  private static final int[] ID3_TAG = new int[] { 0x49, 0x44, 0x33 };

  @Override
  public String getName() {
    return "mp3";
  }

  @Override
  public MediaContainerDetection.Result probe(String identifier, SeekableInputStream inputStream) throws IOException {
    if (!checkNextBytes(inputStream, ID3_TAG)) {
      return null;
    }

    log.debug("Track {} is an MP3 file.", identifier);

    Mp3StreamingFile file = new Mp3StreamingFile(null, inputStream);

    try {
      file.parseHeaders();

      return new MediaContainerDetection.Result(this, new AudioTrackInfo("unknown", "unknown", (int) file.getDuration(), identifier));
    } finally {
      file.close();
    }
  }

  @Override
  public AudioTrack createTrack(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    return new Mp3AudioTrack(trackInfo, inputStream);
  }
}
