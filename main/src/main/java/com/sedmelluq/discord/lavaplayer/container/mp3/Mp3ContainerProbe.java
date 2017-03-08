package com.sedmelluq.discord.lavaplayer.container.mp3;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerProbe;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.STREAM_SCAN_DISTANCE;
import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.UNKNOWN_ARTIST;
import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.UNKNOWN_TITLE;
import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.checkNextBytes;
import static com.sedmelluq.discord.lavaplayer.tools.DataFormatTools.defaultOnNull;

/**
 * Container detection probe for MP3 format.
 */
public class Mp3ContainerProbe implements MediaContainerProbe {
  private static final Logger log = LoggerFactory.getLogger(Mp3ContainerProbe.class);

  private static final int[] ID3_TAG = new int[] { 0x49, 0x44, 0x33 };
  private static final String TITLE_TAG = "TIT2";
  private static final String ARTIST_TAG = "TPE1";

  @Override
  public String getName() {
    return "mp3";
  }

  @Override
  public boolean matchesHints(MediaContainerHints hints) {
    boolean invalidMimeType = hints.mimeType != null && !"audio/mpeg".equalsIgnoreCase(hints.mimeType);
    boolean invalidFileExtension = hints.fileExtension != null && !"mp3".equalsIgnoreCase(hints.mimeType);
    return !invalidMimeType && !invalidFileExtension;
  }

  @Override
  public MediaContainerDetectionResult probe(AudioReference reference, SeekableInputStream inputStream) throws IOException {
    if (!checkNextBytes(inputStream, ID3_TAG)) {
      byte[] frameHeader = new byte[4];
      Mp3FrameReader frameReader = new Mp3FrameReader(inputStream, frameHeader);
      if (!frameReader.scanForFrame(STREAM_SCAN_DISTANCE, false)) {
        return null;
      }

      inputStream.seek(0);
    }

    log.debug("Track {} is an MP3 file.", reference.identifier);

    Mp3TrackProvider file = new Mp3TrackProvider(null, inputStream);

    try {
      file.parseHeaders();

      return new MediaContainerDetectionResult(this, new AudioTrackInfo(
          defaultOnNull(file.getIdv3Tag(TITLE_TAG), reference.title != null ? reference.title : UNKNOWN_TITLE),
          defaultOnNull(file.getIdv3Tag(ARTIST_TAG), UNKNOWN_ARTIST),
          file.getDuration(),
          reference.identifier,
          !file.isSeekable(),
          reference.identifier
      ));
    } finally {
      file.close();
    }
  }

  @Override
  public AudioTrack createTrack(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    return new Mp3AudioTrack(trackInfo, inputStream);
  }
}
