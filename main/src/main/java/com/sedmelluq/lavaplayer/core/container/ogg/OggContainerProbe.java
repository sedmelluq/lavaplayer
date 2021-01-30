package com.sedmelluq.lavaplayer.core.container.ogg;

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

/**
 * Container detection probe for OGG stream.
 */
public class OggContainerProbe implements MediaContainerProbe {
  private static final Logger log = LoggerFactory.getLogger(OggContainerProbe.class);

  @Override
  public String getName() {
    return "ogg";
  }

  @Override
  public boolean matchesHints(MediaContainerHints hints) {
    return false;
  }

  @Override
  public MediaContainerDetectionResult probe(AudioInfoRequest request, SeekableInputStream stream) throws IOException {
    if (!checkNextBytes(stream, OggPacketInputStream.OGG_PAGE_HEADER)) {
      return null;
    }

    log.debug("Track {} is an OGG stream.", request.name());

    AudioTrackInfoBuilder infoBuilder = AudioTrackInfoBuilder
        .fromTemplate(request)
        .withProvider(stream)
        .with(coreIsStream(true));

    try {
      collectStreamInformation(stream, infoBuilder);
    } catch (Exception e) {
      log.warn("Failed to collect additional information on OGG stream.", e);
    }

    return supportedFormat(this, infoBuilder);
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    return new OggStreamPlayback(trackInfo.getIdentifier(), inputStream);
  }

  private void collectStreamInformation(
      SeekableInputStream stream,
      AudioTrackInfoBuilder infoBuilder
  ) throws IOException {
    OggPacketInputStream packetInputStream = new OggPacketInputStream(stream, false);
    OggMetadata metadata = OggTrackLoader.loadMetadata(packetInputStream);

    if (metadata != null) {
      infoBuilder.withProvider(metadata);
    }
  }
}
