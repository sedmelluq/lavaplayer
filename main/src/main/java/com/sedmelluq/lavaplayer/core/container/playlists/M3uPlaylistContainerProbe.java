package com.sedmelluq.lavaplayer.core.container.playlists;

import com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult;
import com.sedmelluq.lavaplayer.core.container.MediaContainerHints;
import com.sedmelluq.lavaplayer.core.container.MediaContainerProbe;
import com.sedmelluq.lavaplayer.core.http.HttpClientTools;
import com.sedmelluq.lavaplayer.core.http.HttpInterfaceManager;
import com.sedmelluq.lavaplayer.core.http.ThreadLocalHttpInterfaceManager;
import com.sedmelluq.lavaplayer.core.info.loader.AudioInfoRequests;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoBuilder;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.source.http.HttpAudioSource;
import com.sedmelluq.lavaplayer.core.tools.DataFormatTools;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetection.checkNextBytes;
import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult.refer;
import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult.supportedFormat;
import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult.unsupportedFormat;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty.Flag.PLAYBACK_CORE;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreIdentifier;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreSourceName;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreTitle;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.custom;

/**
 * Probe for M3U playlist.
 */
public class M3uPlaylistContainerProbe implements MediaContainerProbe {
  private static final Logger log = LoggerFactory.getLogger(M3uPlaylistContainerProbe.class);

  private static final String HLS_LEVEL_PROPERTY = "hls-level";

  private static final String TYPE_HLS_OUTER = "hls-outer";
  private static final String TYPE_HLS_INNER = "hls-inner";

  private static final int[] M3U_HEADER_TAG = new int[] { '#', 'E', 'X', 'T', 'M', '3', 'U' };
  private static final int[] M3U_ENTRY_TAG = new int[] { '#', 'E', 'X', 'T', 'I', 'N', 'F' };

  private final HttpInterfaceManager httpInterfaceManager = new ThreadLocalHttpInterfaceManager(
      HttpClientTools
          .createSharedCookiesHttpBuilder()
          .setRedirectStrategy(new HttpClientTools.NoRedirectsStrategy()),
      HttpClientTools.DEFAULT_REQUEST_CONFIG
  );

  @Override
  public String getName() {
    return "m3u";
  }

  @Override
  public boolean matchesHints(MediaContainerHints hints) {
    return false;
  }

  @Override
  public MediaContainerDetectionResult probe(
      AudioInfoRequest request,
      SeekableInputStream inputStream
  ) throws IOException {
    if (!checkNextBytes(inputStream, M3U_HEADER_TAG) && !checkNextBytes(inputStream, M3U_ENTRY_TAG)) {
      return null;
    }

    log.debug("Track {} is an M3U playlist file.", request.name());
    String[] lines = DataFormatTools.streamToLines(inputStream, StandardCharsets.UTF_8);

    String hlsStreamUrl = HlsStreamSegmentUrlProvider.findHlsEntryUrl(lines);

    if (hlsStreamUrl != null) {
      AudioTrackInfoBuilder builder = AudioTrackInfoBuilder
          .fromTemplate(request)
          .withProvider(inputStream);

      if (HttpAudioSource.extractUrl(request) != null) {
        return supportedFormat(this, builder
            .with(custom(HLS_LEVEL_PROPERTY, PLAYBACK_CORE.mask, TYPE_HLS_OUTER)));
      } else {
        return supportedFormat(this, builder
            .with(coreSourceName("http"))
            .with(coreIdentifier(hlsStreamUrl))
            .with(custom(HLS_LEVEL_PROPERTY, PLAYBACK_CORE.mask, TYPE_HLS_INNER)));
      }
    }

    MediaContainerDetectionResult result = loadSingleItemPlaylist(request, lines);
    if (result != null) {
      return result;
    }

    return unsupportedFormat(this, "The playlist file contains no links.");
  }

  private MediaContainerDetectionResult loadSingleItemPlaylist(AudioInfoRequest request, String[] lines) {
    String trackTitle = null;

    for (String line : lines) {
      if (line.startsWith("#EXTINF")) {
        trackTitle = extractTitleFromInfo(line);
      } else if (!line.startsWith("#") && line.length() > 0) {
        if (line.startsWith("http://") || line.startsWith("https://") || line.startsWith("icy://")) {
          return refer(this, AudioInfoRequests
              .genericBuilder(line.trim())
              .withInheritedFields(request)
              .withProperty(coreTitle(trackTitle))
              .build());
        }

        trackTitle = null;
      }
    }

    return null;
  }

  private String extractTitleFromInfo(String infoLine) {
    String[] splitInfo = infoLine.split(",", 2);
    return splitInfo.length == 2 ? splitInfo[1] : null;
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    String property = trackInfo.getStringProperty(HLS_LEVEL_PROPERTY);

    if (TYPE_HLS_INNER.equals(property)) {
      return new HlsStreamPlayback(trackInfo.getIdentifier(), httpInterfaceManager, true);
    } else if (TYPE_HLS_OUTER.equals(property)) {
      return new HlsStreamPlayback(trackInfo.getIdentifier(), httpInterfaceManager, false);
    } else {
      throw new IllegalArgumentException("Unsupported parameters: " + property);
    }
  }
}
