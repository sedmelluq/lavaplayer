package com.sedmelluq.lavaplayer.core.container.flac;

import com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult;
import com.sedmelluq.lavaplayer.core.container.MediaContainerHints;
import com.sedmelluq.lavaplayer.core.container.MediaContainerProbe;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoBuilder;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetection.checkNextBytes;
import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult.supportedFormat;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreAuthor;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreLength;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreTitle;

/**
 * Container detection probe for MP3 format.
 */
public class FlacContainerProbe implements MediaContainerProbe {
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
  public MediaContainerDetectionResult probe(
      AudioInfoRequest request,
      SeekableInputStream inputStream
  ) throws IOException {
    if (!checkNextBytes(inputStream, FlacFileLoader.FLAC_CC)) {
      return null;
    }

    log.debug("Track {} is a FLAC file.", request.name());

    FlacTrackInfo fileInfo = new FlacFileLoader(inputStream).parseHeaders();

    AudioTrackInfoBuilder builder = AudioTrackInfoBuilder.fromTemplate(request)
        .withProvider(inputStream)
        .with(coreTitle(fileInfo.tags.get(TITLE_TAG)))
        .with(coreAuthor(fileInfo.tags.get(ARTIST_TAG)))
        .with(coreLength(fileInfo.duration));

    return supportedFormat(this, builder);
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    return new FlacPlayback(trackInfo.getIdentifier(), inputStream);
  }
}
