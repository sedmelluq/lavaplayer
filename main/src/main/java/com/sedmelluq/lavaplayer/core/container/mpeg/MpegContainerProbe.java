package com.sedmelluq.lavaplayer.core.container.mpeg;

import com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult;
import com.sedmelluq.lavaplayer.core.container.MediaContainerHints;
import com.sedmelluq.lavaplayer.core.container.MediaContainerProbe;
import com.sedmelluq.lavaplayer.core.container.mpeg.reader.MpegFileTrackProvider;
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
import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult.unsupportedFormat;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreAuthor;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreLength;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreTitle;

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
  public boolean matchesHints(MediaContainerHints hints) {
    return false;
  }

  @Override
  public MediaContainerDetectionResult probe(
      AudioInfoRequest request,
      SeekableInputStream inputStream
  ) throws IOException {
    if (!checkNextBytes(inputStream, ISO_TAG)) {
      return null;
    }

    log.debug("Track {} is an MP4 file.", request.name());

    MpegFileLoader file = new MpegFileLoader(inputStream);
    file.parseHeaders();

    MpegTrackInfo audioTrack = getSupportedAudioTrack(file);

    if (audioTrack == null) {
      return unsupportedFormat(this, "No supported audio format in the MP4 file.");
    }

    MpegTrackConsumer trackConsumer = new MpegNoopTrackConsumer(audioTrack);
    MpegFileTrackProvider fileReader = file.loadReader(trackConsumer);

    if (fileReader == null) {
      return unsupportedFormat(this, "MP4 file uses an unsupported format.");
    }

    AudioTrackInfoBuilder builder = AudioTrackInfoBuilder
        .fromTemplate(request)
        .with(coreTitle(file.getTextMetadata("Title")))
        .with(coreAuthor(file.getTextMetadata("Artist")))
        .with(coreLength(fileReader.getDuration()));

    return supportedFormat(this, builder);
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    return new MpegStreamPlayback(inputStream);
  }

  private MpegTrackInfo getSupportedAudioTrack(MpegFileLoader file) {
    for (MpegTrackInfo track : file.getTrackList()) {
      if ("soun".equals(track.handler) && "mp4a".equals(track.codecName)) {
        return track;
      }
    }

    return null;
  }
}
