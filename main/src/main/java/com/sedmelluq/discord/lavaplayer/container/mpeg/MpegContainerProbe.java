package com.sedmelluq.discord.lavaplayer.container.mpeg;

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
 * Container detection probe for MP4 format.
 */
public class MpegContainerProbe implements MediaContainerProbe {
  private static final Logger log = LoggerFactory.getLogger(MpegContainerProbe.class);

  private static final int[] ISO_TAG = new int[] { 0x00, 0x00, 0x00, -1, 0x66, 0x74, 0x79, 0x70 };

  @Override
  public String getName() {
    return "mp4";
  }

  @Override
  public MediaContainerDetection.Result probe(String identifier, SeekableInputStream inputStream) throws IOException {
    if (!checkNextBytes(inputStream, ISO_TAG)) {
      return null;
    }

    log.debug("Track {} is an MP4 file.", identifier);

    MpegStreamingFile file = new MpegStreamingFile(inputStream);

    if (!file.isFragmented()) {
      return new MediaContainerDetection.Result(this, "Only fragmented MP4 file format is currently supported.");
    } else if (!hasSupportedAudioTrack(file)) {
      return new MediaContainerDetection.Result(this, "No supported audio format in the MP4 file.");
    }

    return new MediaContainerDetection.Result(this, new AudioTrackInfo("unknown", "unknown", (int) file.getDuration(), identifier));
  }

  @Override
  public AudioTrack createTrack(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    return new MpegAudioTrack(trackInfo, inputStream);
  }

  private boolean hasSupportedAudioTrack(MpegStreamingFile file) {
    for (MpegTrackInfo track : file.getTrackList()) {
      if ("soun".equals(track.handler) && "mp4a".equals(track.codecName)) {
        return true;
      }
    }

    return false;
  }
}
