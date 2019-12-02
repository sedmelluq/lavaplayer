package com.sedmelluq.lavaplayer.core.container.matroska;

import com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult;
import com.sedmelluq.lavaplayer.core.container.MediaContainerHints;
import com.sedmelluq.lavaplayer.core.container.MediaContainerProbe;
import com.sedmelluq.lavaplayer.core.container.matroska.format.MatroskaFileTrack;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoBuilder;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetection.checkNextBytes;
import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult.supportedFormat;
import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult.unsupportedFormat;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreIsStream;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreLength;

/**
 * Container detection probe for matroska format.
 */
public class MatroskaContainerProbe implements MediaContainerProbe {
  private static final Logger log = LoggerFactory.getLogger(MatroskaContainerProbe.class);

  public static final String OPUS_CODEC = "A_OPUS";
  public static final String VORBIS_CODEC = "A_VORBIS";
  public static final String AAC_CODEC = "A_AAC";

  private static final int[] EBML_TAG = new int[] { 0x1A, 0x45, 0xDF, 0xA3 };
  private static final List<String> supportedCodecs = Arrays.asList(OPUS_CODEC, VORBIS_CODEC, AAC_CODEC);

  @Override
  public String getName() {
    return "matroska/webm";
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
    if (!checkNextBytes(inputStream, EBML_TAG)) {
      return null;
    }

    log.debug("Track {} is a matroska file.", request.name());

    MatroskaStreamingFile file = new MatroskaStreamingFile(inputStream);
    file.readFile();

    if (!hasSupportedAudioTrack(file)) {
      return unsupportedFormat(this, "No supported audio tracks present in the file.");
    }

    AudioTrackInfoBuilder builder = AudioTrackInfoBuilder
        .fromTemplate(request)
        .with(coreLength((long) file.getDuration()))
        .with(coreIsStream(false));

    return supportedFormat(this, builder);
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    return new MatroskaStreamPlayback(inputStream);
  }

  private boolean hasSupportedAudioTrack(MatroskaStreamingFile file) {
    for (MatroskaFileTrack track : file.getTrackList()) {
      if (track.type == MatroskaFileTrack.Type.AUDIO && supportedCodecs.contains(track.codecId)) {
        return true;
      }
    }

    return false;
  }
}
