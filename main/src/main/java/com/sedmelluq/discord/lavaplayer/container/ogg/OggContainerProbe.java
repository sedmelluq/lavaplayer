package com.sedmelluq.discord.lavaplayer.container.ogg;

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
import static com.sedmelluq.discord.lavaplayer.container.ogg.OggPacketInputStream.OGG_PAGE_HEADER;
import static com.sedmelluq.discord.lavaplayer.tools.DataFormatTools.defaultOnNull;

/**
 * Container detection probe for OGG stream.
 */
public class OggContainerProbe extends AbstractMediaContainerProbe {
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

    return new MediaContainerDetectionResult(this, new AudioTrackInfo(
        reference.title != null ? reference.title : getDefaultTitle(stream),
        getDefaultArtist(stream),
        Long.MAX_VALUE,
        reference.identifier,
        true,
        defaultOnNull(getDefaultUrl(stream), reference.identifier)
    ));
  }

  @Override
  public AudioTrack createTrack(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    return new OggAudioTrack(trackInfo, inputStream);
  }
}
