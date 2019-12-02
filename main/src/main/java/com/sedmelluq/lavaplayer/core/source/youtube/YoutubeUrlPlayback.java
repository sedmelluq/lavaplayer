package com.sedmelluq.lavaplayer.core.source.youtube;

import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoTemplate;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.container.matroska.MatroskaStreamPlayback;
import com.sedmelluq.lavaplayer.core.container.mpeg.MpegStreamPlayback;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import java.net.URI;
import java.util.List;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.container.Formats.MIME_AUDIO_WEBM;
import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.COMMON;

public class YoutubeUrlPlayback implements AudioPlayback {
  private static final Logger log = LoggerFactory.getLogger(YoutubeUrlPlayback.class);

  private final YoutubeAudioSource sourceManager;
  private final AudioTrackInfo trackInfo;

  public YoutubeUrlPlayback(YoutubeAudioSource sourceManager, AudioTrackInfo trackInfo) {
    this.sourceManager = sourceManager;
    this.trackInfo = trackInfo;
  }

  @Override
  public void process(AudioPlaybackController controller) {
    try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
      FormatWithUrl format = loadBestFormatWithUrl(httpInterface);

      log.debug("Starting track from URL: {}", format.signedUrl);

      if (trackInfo.isStream()) {
        processStream(controller, format);
      } else {
        processStatic(controller, httpInterface, format);
      }
    } catch (Exception e) {
      throw ExceptionTools.toRuntimeException(e);
    }
  }

  private void processStatic(AudioPlaybackController controller, HttpInterface httpInterface, FormatWithUrl format) throws Exception {
    try (YoutubePersistentHttpStream stream = new YoutubePersistentHttpStream(httpInterface, format.signedUrl, format.details.getContentLength())) {
      if (format.details.getType().getMimeType().endsWith("/webm")) {
        new MatroskaStreamPlayback(stream).process(controller);
      } else {
        new MpegStreamPlayback(stream).process(controller);
      }
    }
  }

  private void processStream(AudioPlaybackController controller, FormatWithUrl format) throws Exception {
    if (MIME_AUDIO_WEBM.equals(format.details.getType().getMimeType())) {
      throw new FriendlyException("YouTube WebM streams are currently not supported.", COMMON, null);
    } else {
      try (HttpInterface streamingInterface = sourceManager.getHttpInterface()) {
        new YoutubeMpegStreamUrlPlayback(streamingInterface, format.signedUrl).process(controller);
      }
    }
  }

  private FormatWithUrl loadBestFormatWithUrl(HttpInterface httpInterface) throws Exception {
    YoutubeTrackDetails details = sourceManager.getTrackDetailsLoader()
        .loadDetails(httpInterface, trackInfo.getIdentifier(), null);

    List<YoutubeTrackFormat> formats = details.getFormats(httpInterface, sourceManager.getSignatureResolver());;

    YoutubeTrackFormat format = findBestSupportedFormat(formats);

    URI signedUrl = sourceManager.getSignatureResolver()
        .resolveFormatUrl(httpInterface, details.getPlayerScript(), format);

    return new FormatWithUrl(format, signedUrl);
  }

  private static boolean isBetterFormat(YoutubeTrackFormat format, YoutubeTrackFormat other) {
    YoutubeFormatInfo info = format.getInfo();

    if (info == null) {
      return false;
    } else if (other == null) {
      return true;
    } else if (info.ordinal() != other.getInfo().ordinal()) {
      return info.ordinal() < other.getInfo().ordinal();
    } else {
      return format.getBitrate() > other.getBitrate();
    }
  }

  private static YoutubeTrackFormat findBestSupportedFormat(List<YoutubeTrackFormat> formats) {
    YoutubeTrackFormat bestFormat = null;

    for (YoutubeTrackFormat format : formats) {
      if (isBetterFormat(format, bestFormat)) {
        bestFormat = format;
      }
    }

    if (bestFormat == null) {
      StringJoiner joiner = new StringJoiner(", ");
      formats.forEach(format -> joiner.add(format.getType().toString()));
      throw new IllegalStateException("No supported audio streams available, available types: " + joiner.toString());
    }

    return bestFormat;
  }

  private static class FormatWithUrl {
    private final YoutubeTrackFormat details;
    private final URI signedUrl;

    private FormatWithUrl(YoutubeTrackFormat details, URI signedUrl) {
      this.details = details;
      this.signedUrl = signedUrl;
    }
  }
}
