package com.sedmelluq.discord.lavaplayer.container.ogg;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerProbe;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.checkNextBytes;
import static com.sedmelluq.discord.lavaplayer.container.ogg.OggPacketInputStream.OGG_PAGE_HEADER;

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
  public MediaContainerDetectionResult probe(AudioReference reference, SeekableInputStream stream) throws IOException {
    if (!checkNextBytes(stream, OGG_PAGE_HEADER)) {
      return null;
    }

    log.debug("Track {} is an OGG stream.", reference.identifier);

    AudioTrackInfoBuilder infoBuilder = AudioTrackInfoBuilder.create(reference, stream).setIsStream(true);

    try {
      collectStreamInformation(stream, infoBuilder);
    } catch (Exception e) {
      log.warn("Failed to collect additional information on OGG stream.", e);
    }

    return new MediaContainerDetectionResult(this, infoBuilder.build());
  }

  @Override
  public AudioTrack createTrack(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    return new OggAudioTrack(trackInfo, inputStream);
  }

  private void collectStreamInformation(SeekableInputStream stream, AudioTrackInfoBuilder infoBuilder) throws IOException {
    OggPacketInputStream packetInputStream = new OggPacketInputStream(stream);
    OggTrackProvider track = OggTrackLoader.loadTrack(packetInputStream);

    if (track != null) {
      try {
        infoBuilder.apply(track.getMetadata());

        OggStreamSizeInfo sizeInfo = track.seekForSizeInfo();

        if (sizeInfo != null) {
          infoBuilder.setLength(sizeInfo.totalSamples * 1000 / sizeInfo.sampleRate);
        }
      } finally {
        track.close();
      }
    }
  }
}
