package com.sedmelluq.discord.lavaplayer.container.wav;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerProbe;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.UNKNOWN_ARTIST;
import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.UNKNOWN_TITLE;
import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.checkNextBytes;
import static com.sedmelluq.discord.lavaplayer.container.wav.WavFileLoader.WAV_RIFF_HEADER;
import static com.sedmelluq.discord.lavaplayer.tools.DataFormatTools.defaultOnNull;

/**
 * Container detection probe for WAV format.
 */
public class WavContainerProbe implements MediaContainerProbe {
  private static final Logger log = LoggerFactory.getLogger(WavContainerProbe.class);

  @Override
  public String getName() {
    return "wav";
  }

  @Override
  public boolean matchesHints(MediaContainerHints hints) {
    return false;
  }

  @Override
  public MediaContainerDetectionResult probe(AudioReference reference, SeekableInputStream inputStream) throws IOException {
    if (!checkNextBytes(inputStream, WAV_RIFF_HEADER)) {
      return null;
    }

    log.debug("Track {} is a WAV file.", reference.identifier);

    WavFileInfo fileInfo = new WavFileLoader(inputStream).parseHeaders();

    return new MediaContainerDetectionResult(this, new AudioTrackInfo(
        defaultOnNull(reference.title, UNKNOWN_TITLE),
        UNKNOWN_ARTIST,
        fileInfo.getDuration(),
        reference.identifier,
        false,
        reference.identifier
    ));
  }

  @Override
  public AudioTrack createTrack(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    return new WavAudioTrack(trackInfo, inputStream);
  }
}
