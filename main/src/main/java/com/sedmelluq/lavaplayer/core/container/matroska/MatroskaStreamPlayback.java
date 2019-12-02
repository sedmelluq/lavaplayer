package com.sedmelluq.lavaplayer.core.container.matroska;

import com.sedmelluq.lavaplayer.core.container.matroska.format.MatroskaFileTrack;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackContext;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatroskaStreamPlayback implements AudioPlayback {
  private static final Logger log = LoggerFactory.getLogger(MatroskaStreamPlayback.class);

  private final SeekableInputStream inputStream;

  public MatroskaStreamPlayback(SeekableInputStream inputStream) {
    this.inputStream = inputStream;
  }

  @Override
  public void process(AudioPlaybackController controller) {
    MatroskaStreamingFile file = loadMatroskaFile(controller);
    MatroskaTrackConsumer trackConsumer = loadAudioTrack(file, controller.getContext());

    try {
      controller.executeProcessingLoop(() -> {
        file.provideFrames(trackConsumer);
      }, position -> {
        file.seekToTimecode(trackConsumer.getTrack().index, position);
      });
    } finally {
      ExceptionTools.closeWithWarnings(trackConsumer);
    }
  }

  private MatroskaStreamingFile loadMatroskaFile(AudioPlaybackController controller) {
    try {
      MatroskaStreamingFile file = new MatroskaStreamingFile(inputStream);
      file.readFile();

      controller.updateDuration((long) file.getDuration());
      return file;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private MatroskaTrackConsumer loadAudioTrack(MatroskaStreamingFile file, AudioPlaybackContext context) {
    MatroskaTrackConsumer trackConsumer = null;
    boolean success = false;

    try {
      trackConsumer = selectAudioTrack(file.getTrackList(), context);

      if (trackConsumer == null) {
        throw new IllegalStateException("No supported audio tracks in the file.");
      } else {
        log.debug("Starting to play track with codec {}", trackConsumer.getTrack().codecId);
      }

      trackConsumer.initialise();
      success = true;
    } finally {
      if (!success && trackConsumer != null) {
        ExceptionTools.closeWithWarnings(trackConsumer);
      }
    }

    return trackConsumer;
  }

  private MatroskaTrackConsumer selectAudioTrack(MatroskaFileTrack[] tracks, AudioPlaybackContext context) {
    MatroskaTrackConsumer trackConsumer = null;

    for (MatroskaFileTrack track : tracks) {
      if (track.type == MatroskaFileTrack.Type.AUDIO) {
        if (MatroskaCodecIds.OPUS_CODEC.equals(track.codecId)) {
          trackConsumer = new MatroskaOpusTrackConsumer(context, track);
          break;
        } else if (MatroskaCodecIds.VORBIS_CODEC.equals(track.codecId)) {
          trackConsumer = new MatroskaVorbisTrackConsumer(context, track);
        } else if (MatroskaCodecIds.AAC_CODEC.equals(track.codecId)) {
          trackConsumer = new MatroskaAacTrackConsumer(context, track);
        }
      }
    }

    return trackConsumer;
  }
}
