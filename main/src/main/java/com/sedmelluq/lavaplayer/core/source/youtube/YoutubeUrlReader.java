package com.sedmelluq.lavaplayer.core.source.youtube;

import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.container.Formats.MIME_AUDIO_WEBM;
import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.COMMON;

public class YoutubeUrlReader {
  private static final Logger log = LoggerFactory.getLogger(YoutubeUrlReader.class);

  private final YoutubeAudioSource sourceManager;
  private final AudioTrackInfo trackInfo;
  private final Handler handler;

  public YoutubeUrlReader(YoutubeAudioSource sourceManager, AudioTrackInfo trackInfo, Handler handler) {
    this.sourceManager = sourceManager;
    this.trackInfo = trackInfo;
    this.handler = handler;
  }

  public void read() {
    try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
      processFormats(httpInterface);
    } catch (Exception e) {
      throw ExceptionTools.toRuntimeException(e);
    }
  }

  public interface Handler {
    void handleFormats(List<YoutubeTrackFormat> formats, FormatReader reader);
  }

  @FunctionalInterface
  public interface FormatReader {
    void read(YoutubeTrackFormat reader, ReadHandler handler);
  }

  public interface ReadHandler {
    void consumeStream(YoutubeTrackFormat format, URI url, SeekableInputStream stream);

    void handleLiveStream(YoutubeTrackFormat format, LiveStreamReader reader);
  }

  public interface LiveStreamReader {
    void read(LiveStreamHandler handler);
  }

  public interface LiveStreamHandler {
    Long consumeSegmentStream(URI url, SeekableInputStream stream);
  }

  private void processFormats(HttpInterface httpInterface) {
    YoutubeTrackDetails details = sourceManager.getTrackDetailsLoader()
        .loadDetails(httpInterface, trackInfo.getIdentifier(), null, true);

    List<YoutubeTrackFormat> formats = details.getFormats(httpInterface, sourceManager.getSignatureResolver());

    handler.handleFormats(formats, (format, readHandler) ->
        readFormat(httpInterface, format, details.getPlayerScript(), readHandler)
    );
  }

  private void readFormat(
      HttpInterface httpInterface,
      YoutubeTrackFormat format,
      String playerScriptUrl,
      ReadHandler readHandler
  ) {
    try {
      FormatWithUrl formatWithUrl = new FormatWithUrl(
          format,
          sourceManager.getSignatureResolver().resolveFormatUrl(httpInterface, playerScriptUrl, format)
      );

      log.debug("Starting to read format {} from URL: {}", format.getType(), formatWithUrl.signedUrl);

      if (trackInfo.isStream()) {
        processStream(formatWithUrl, readHandler);
      } else {
        processStatic(httpInterface, formatWithUrl, readHandler);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void processStatic(
      HttpInterface httpInterface,
      FormatWithUrl format,
      ReadHandler readHandler
  ) throws Exception {
    try (YoutubePersistentHttpStream stream = new YoutubePersistentHttpStream(
        httpInterface,
        format.signedUrl,
        format.details.getContentLength()
    )) {
      readHandler.consumeStream(format.details, format.signedUrl, stream);
    }
  }

  private void processStream(FormatWithUrl format, ReadHandler readHandler) throws Exception {
    if (MIME_AUDIO_WEBM.equals(format.details.getType().getMimeType())) {
      throw new FriendlyException("YouTube WebM streams are currently not supported.", COMMON, null);
    } else {
      try (HttpInterface streamingInterface = sourceManager.getHttpInterface()) {
        readHandler.handleLiveStream(format.details, liveStreamHandler ->
            new YoutubeMpegStreamUrlReader(streamingInterface, format.signedUrl, liveStreamHandler).read()
        );
      }
    }
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
