package com.sedmelluq.discord.lavaplayer.container.mpeg;

import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BaseAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Audio track that handles the processing of MP4 format
 */
public class MpegAudioTrack extends BaseAudioTrack {
  private static final Logger log = LoggerFactory.getLogger(MpegAudioTrack.class);

  private final SeekableInputStream inputStream;

  /**
   * @param executor Track executor associated with the current track
   * @param trackInfo Track info
   * @param inputStream Input stream for the MP4 file
   */
  public MpegAudioTrack(AudioTrackExecutor executor, AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    super(executor, trackInfo);

    this.inputStream = inputStream;
  }

  @Override
  public void process(AtomicInteger volumeLevel) {
    MpegStreamingFile file = loadMp4File();
    MpegTrackConsumer trackConsumer = loadAudioTrack(file, volumeLevel);

    try {
      executor.executeProcessingLoop(() -> file.provideFrames(trackConsumer), file::seekToTimecode);
    } finally {
      trackConsumer.close();
    }
  }

  private MpegStreamingFile loadMp4File() {
    MpegStreamingFile file = new MpegStreamingFile(inputStream);
    file.readFile();

    if (!file.isFragmented()) {
      throw new FriendlyException("This track uses an unsupported MP4 version.",
          new IllegalStateException("Non-fragmented MP4 files are not supported."));
    }

    accurateDuration.set(file.getDuration());

    return file;
  }

  private MpegTrackConsumer loadAudioTrack(MpegStreamingFile file, AtomicInteger volumeLevel) {
    MpegTrackConsumer trackConsumer = null;
    boolean success = false;

    try {
      trackConsumer = selectAudioTrack(file.getTrackList(), volumeLevel);

      if (trackConsumer == null) {
        throw new FriendlyException("The audio codec used in the track is not supported.", null);
      } else {
        log.debug("Starting to play track with codec {}", trackConsumer.getTrack().codecName);
      }

      trackConsumer.initialise();
      success = true;
      return trackConsumer;
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions("Something went wrong when loading an MP4 format track.", e);
    } finally {
      if (!success && trackConsumer != null) {
        trackConsumer.close();
      }
    }
  }

  private MpegTrackConsumer selectAudioTrack(List<MpegTrackInfo> tracks, AtomicInteger volumeLevel) {
    for (MpegTrackInfo track : tracks) {
      if ("soun".equals(track.handler) && "mp4a".equals(track.codecName)) {
        return new MpegAacTrackConsumer(track, executor.getFrameConsumer(), volumeLevel);
      }
    }

    return null;
  }
}
