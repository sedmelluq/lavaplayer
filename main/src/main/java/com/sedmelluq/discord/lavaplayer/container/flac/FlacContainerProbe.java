package com.sedmelluq.discord.lavaplayer.container.flac;

import com.sedmelluq.discord.lavaplayer.container.AbstractMediaContainerProbe;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.checkNextBytes;
import static com.sedmelluq.discord.lavaplayer.tools.DataFormatTools.defaultOnNull;

/**
 * Container detection probe for MP3 format.
 */
public class FlacContainerProbe extends AbstractMediaContainerProbe {
  private static final Logger log = LoggerFactory.getLogger(FlacContainerProbe.class);

  private static final String TITLE_TAG = "TITLE";
  private static final String ARTIST_TAG = "ARTIST";

  @Override
  public String getName() {
    return "flac";
  }

  @Override
  public boolean matchesHints(MediaContainerHints hints) {
    return false;
  }

  @Override
  public MediaContainerDetectionResult probe(AudioReference reference, SeekableInputStream inputStream) throws IOException {
    if (!checkNextBytes(inputStream, FlacFileLoader.FLAC_CC)) {
      return null;
    }

    log.debug("Track {} is a FLAC file.", reference.identifier);

    FlacTrackInfo trackInfo = new FlacFileLoader(inputStream).parseHeaders();

    return new MediaContainerDetectionResult(this, new AudioTrackInfo(
        defaultOnNull(trackInfo.tags.get(TITLE_TAG), getDefaultTitle(inputStream)),
        defaultOnNull(trackInfo.tags.get(ARTIST_TAG), getDefaultArtist(inputStream)),
        trackInfo.duration,
        reference.identifier,
        false,
        defaultOnNull(getDefaultUrl(inputStream), reference.identifier)
    ));
  }

  @Override
  public AudioTrack createTrack(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    return new FlacAudioTrack(trackInfo, inputStream);
  }
}
