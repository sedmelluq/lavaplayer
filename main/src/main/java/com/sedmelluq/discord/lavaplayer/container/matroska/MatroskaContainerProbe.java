package com.sedmelluq.discord.lavaplayer.container.matroska;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerProbe;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.ebml.matroska.MatroskaFileTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.UNKNOWN_ARTIST;
import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.UNKNOWN_TITLE;
import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.checkNextBytes;

/**
 * Container detection probe for matroska format.
 */
public class MatroskaContainerProbe implements MediaContainerProbe {
  private static final Logger log = LoggerFactory.getLogger(MatroskaContainerProbe.class);

  static final String OPUS_CODEC = "A_OPUS";
  static final String VORBIS_CODEC = "A_VORBIS";
  static final String AAC_CODEC = "A_AAC";

  private static final int[] EBML_TAG = new int[] { 0x1A, 0x45, 0xDF, 0xA3 };
  private static final List<String> supportedCodecs = Arrays.asList(OPUS_CODEC, VORBIS_CODEC, AAC_CODEC);

  @Override
  public String getName() {
    return "matroska/webm";
  }

  @Override
  public MediaContainerDetection.Result probe(String identifier, SeekableInputStream inputStream) throws IOException {
    if (!checkNextBytes(inputStream, EBML_TAG)) {
      return null;
    }

    log.debug("Track {} is a matroska file.", identifier);

    MatroskaStreamingFile file = new MatroskaStreamingFile(new MatroskaStreamDataSource(inputStream));
    file.readFile();

    if (!hasSupportedAudioTrack(file)) {
      return new MediaContainerDetection.Result(this, "No supported audio tracks present in the file.");
    }

    return new MediaContainerDetection.Result(this, new AudioTrackInfo(UNKNOWN_TITLE, UNKNOWN_ARTIST, (int) file.getDuration(), identifier));
  }

  private boolean hasSupportedAudioTrack(MatroskaStreamingFile file) {
    for (MatroskaFileTrack track : file.getTrackList()) {
      if (track.getTrackType() == MatroskaFileTrack.TrackType.AUDIO && supportedCodecs.contains(track.getCodecID())) {
        return true;
      }
    }

    return false;
  }

  @Override
  public AudioTrack createTrack(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    return new MatroskaAudioTrack(trackInfo, inputStream);
  }
}
