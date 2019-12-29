package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.container.matroska.MatroskaAudioTrack;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import java.net.URI;
import java.util.List;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.discord.lavaplayer.container.Formats.MIME_AUDIO_WEBM;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;

/**
 * Audio track that handles processing Youtube videos as audio tracks.
 */
public class YoutubeAudioTrack extends DelegatedAudioTrack {
  private static final Logger log = LoggerFactory.getLogger(YoutubeAudioTrack.class);

  private final YoutubeAudioSourceManager sourceManager;

  /**
   * @param trackInfo Track info
   * @param sourceManager Source manager which was used to find this track
   */
  public YoutubeAudioTrack(AudioTrackInfo trackInfo, YoutubeAudioSourceManager sourceManager) {
    super(trackInfo);

    this.sourceManager = sourceManager;
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
    try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
      FormatWithUrl format = loadBestFormatWithUrl(httpInterface);

      log.debug("Starting track from URL: {}", format.signedUrl);

      if (trackInfo.isStream) {
        processStream(localExecutor, format);
      } else {
        processStatic(localExecutor, httpInterface, format);
      }
    }
  }

  private void processStatic(LocalAudioTrackExecutor localExecutor, HttpInterface httpInterface, FormatWithUrl format) throws Exception {
    try (YoutubePersistentHttpStream stream = new YoutubePersistentHttpStream(httpInterface, format.signedUrl, format.details.getContentLength())) {
      if (format.details.getType().getMimeType().endsWith("/webm")) {
        processDelegate(new MatroskaAudioTrack(trackInfo, stream), localExecutor);
      } else {
        processDelegate(new MpegAudioTrack(trackInfo, stream), localExecutor);
      }
    }
  }

  private void processStream(LocalAudioTrackExecutor localExecutor, FormatWithUrl format) throws Exception {
    if (MIME_AUDIO_WEBM.equals(format.details.getType().getMimeType())) {
      throw new FriendlyException("YouTube WebM streams are currently not supported.", COMMON, null);
    } else {
      try (HttpInterface streamingInterface = sourceManager.getHttpInterface()) {
        processDelegate(new YoutubeMpegStreamAudioTrack(trackInfo, streamingInterface, format.signedUrl), localExecutor);
      }
    }
  }

  private FormatWithUrl loadBestFormatWithUrl(HttpInterface httpInterface) throws Exception {
    YoutubeTrackDetails details = sourceManager.getTrackDetailsLoader().loadDetails(httpInterface, getIdentifier());

    // If the error reason is "Video unavailable" details will return null
    if (details == null) {
      throw new FriendlyException("This video is not available", FriendlyException.Severity.COMMON, null);
    }

    List<YoutubeTrackFormat> formats = details.getFormats(httpInterface, sourceManager.getSignatureResolver());;

    YoutubeTrackFormat format = findBestSupportedFormat(formats);

    URI signedUrl = sourceManager.getSignatureResolver()
        .resolveFormatUrl(httpInterface, details.getPlayerScript(), format);

    return new FormatWithUrl(format, signedUrl);
  }

  @Override
  protected AudioTrack makeShallowClone() {
    return new YoutubeAudioTrack(trackInfo, sourceManager);
  }

  @Override
  public AudioSourceManager getSourceManager() {
    return sourceManager;
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
