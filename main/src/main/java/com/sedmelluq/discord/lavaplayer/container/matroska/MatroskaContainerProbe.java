package com.sedmelluq.discord.lavaplayer.container.matroska;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerProbe;
import com.sedmelluq.discord.lavaplayer.container.matroska.format.MatroskaFileTrack;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.UNKNOWN_ARTIST;
import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.UNKNOWN_TITLE;
import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.checkNextBytes;
import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult.supportedFormat;
import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult.unsupportedFormat;

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
  public MediaContainerDetectionResult probe(AudioReference reference, SeekableInputStream inputStream) throws IOException {
    if (!checkNextBytes(inputStream, EBML_TAG)) {
      return null;
    }

    log.debug("Track {} is a matroska file.", reference.identifier);

    MatroskaStreamingFile file = new MatroskaStreamingFile(inputStream);
    file.readFile();

    if (!hasSupportedAudioTrack(file)) {
      return unsupportedFormat(this, "No supported audio tracks present in the file.");
    }

    return supportedFormat(this, null, new AudioTrackInfo(UNKNOWN_TITLE, UNKNOWN_ARTIST,
        (long) file.getDuration(), reference.identifier, false, reference.identifier));
  }

  private boolean hasSupportedAudioTrack(MatroskaStreamingFile file) {
    for (MatroskaFileTrack track : file.getTrackList()) {
      if (track.type == MatroskaFileTrack.Type.AUDIO && supportedCodecs.contains(track.codecId)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public AudioTrack createTrack(String parameters, AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    return new MatroskaAudioTrack(trackInfo, inputStream);
  }
}
