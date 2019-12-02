package com.sedmelluq.lavaplayer.core.container.mp3;

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

import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetection.STREAM_SCAN_DISTANCE;
import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetection.checkNextBytes;
import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult.supportedFormat;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreIsStream;

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
  public boolean matchesHints(MediaContainerHints hints) {
    boolean invalidMimeType = hints.mimeType != null && !"audio/mpeg".equalsIgnoreCase(hints.mimeType);
    boolean invalidFileExtension = hints.fileExtension != null && !"mp3".equalsIgnoreCase(hints.fileExtension);
    return hints.present() && !invalidMimeType && !invalidFileExtension;
  }

  @Override
  public MediaContainerDetectionResult probe(AudioInfoRequest request, SeekableInputStream inputStream) throws IOException {
    if (!checkNextBytes(inputStream, ID3_TAG)) {
      byte[] frameHeader = new byte[4];
      Mp3FrameReader frameReader = new Mp3FrameReader(inputStream, frameHeader);
      if (!frameReader.scanForFrame(STREAM_SCAN_DISTANCE, false)) {
        return null;
      }

      inputStream.seek(0);
    }

    log.debug("Track {} is an MP3 file.", request.name());

    Mp3TrackProvider file = new Mp3TrackProvider(null, inputStream);

    try {
      file.parseHeaders();

      AudioTrackInfoBuilder builder = AudioTrackInfoBuilder
          .fromTemplate(request)
          .withProvider(file)
          .with(coreIsStream(!file.isSeekable()));

      return supportedFormat(this, builder);
    } finally {
      file.close();
    }
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    return new Mp3StreamPlayback(trackInfo.getIdentifier(), inputStream);
  }
}
