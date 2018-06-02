package com.sedmelluq.discord.lavaplayer.container.mpeg;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerProbe;
import com.sedmelluq.discord.lavaplayer.container.mpeg.reader.MpegFileTrackProvider;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.checkNextBytes;

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
  public MediaContainerDetectionResult probe(AudioReference reference, SeekableInputStream inputStream) throws IOException {
    if (!checkNextBytes(inputStream, ISO_TAG)) {
      return null;
    }

    log.debug("Track {} is an MP4 file.", reference.identifier);

    MpegFileLoader file = new MpegFileLoader(inputStream);
    file.parseHeaders();

    MpegTrackInfo audioTrack = getSupportedAudioTrack(file);

    if (audioTrack == null) {
      return new MediaContainerDetectionResult(this, "No supported audio format in the MP4 file.");
    }

    MpegTrackConsumer trackConsumer = new MpegNoopTrackConsumer(audioTrack);
    MpegFileTrackProvider fileReader = file.loadReader(trackConsumer);

    if (fileReader == null) {
      return new MediaContainerDetectionResult(this, "MP4 file uses an unsupported format.");
    }

    AudioTrackInfo trackInfo = AudioTrackInfoBuilder.create(reference, inputStream)
        .setTitle(file.getTextMetadata("Title"))
        .setAuthor(file.getTextMetadata("Artist"))
        .setLength(fileReader.getDuration())
        .build();

    return new MediaContainerDetectionResult(this, trackInfo);
  }

  @Override
  public AudioTrack createTrack(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    return new MpegAudioTrack(trackInfo, inputStream);
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
