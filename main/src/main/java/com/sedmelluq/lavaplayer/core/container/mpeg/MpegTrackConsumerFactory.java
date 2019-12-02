package com.sedmelluq.lavaplayer.core.container.mpeg;

import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackContext;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.FAULT;
import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.SUSPICIOUS;

public class MpegTrackConsumerFactory {
  private static final Logger log = LoggerFactory.getLogger(MpegTrackConsumerFactory.class);

  public static MpegTrackConsumer create(MpegFileLoader file, AudioPlaybackContext context) {
    MpegTrackConsumer trackConsumer = null;
    boolean success = false;

    try {
      trackConsumer = selectAudioTrack(file.getTrackList(), context);

      if (trackConsumer == null) {
        throw new FriendlyException("The audio codec used in the track is not supported.", SUSPICIOUS, null);
      } else {
        log.debug("Starting to play track with codec {}", trackConsumer.getTrack().codecName);
      }

      trackConsumer.initialise();
      success = true;
      return trackConsumer;
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions("Something went wrong when loading an MP4 format track.", FAULT, e);
    } finally {
      if (!success && trackConsumer != null) {
        trackConsumer.close();
      }
    }
  }

  private static MpegTrackConsumer selectAudioTrack(List<MpegTrackInfo> tracks, AudioPlaybackContext context) {
    for (MpegTrackInfo track : tracks) {
      if ("soun".equals(track.handler) && "mp4a".equals(track.codecName)) {
        return new MpegAacTrackConsumer(context, track);
      }
    }

    return null;
  }
}
