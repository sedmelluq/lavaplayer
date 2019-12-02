package com.sedmelluq.lavaplayer.core.container.wav;

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
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreIsStream;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreLength;

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
  public MediaContainerDetectionResult probe(AudioInfoRequest request, SeekableInputStream inputStream) throws IOException {
    if (!checkNextBytes(inputStream, WavFileLoader.WAV_RIFF_HEADER)) {
      return null;
    }

    log.debug("Track {} is a WAV file.", request.name());

    WavFileInfo fileInfo = new WavFileLoader(inputStream).parseHeaders();

    return supportedFormat(this, AudioTrackInfoBuilder
        .fromTemplate(request)
        .withProvider(inputStream)
        .with(coreLength(fileInfo.getDuration()))
        .with(coreIsStream(false))
    );
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    return new WavStreamPlayback(trackInfo.getIdentifier(), inputStream);
  }
}
