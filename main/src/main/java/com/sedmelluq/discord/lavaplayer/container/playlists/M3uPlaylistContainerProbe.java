package com.sedmelluq.discord.lavaplayer.container.playlists;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDescriptor;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerProbe;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.ThreadLocalHttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.checkNextBytes;
import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult.refer;
import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult.supportedFormat;
import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult.unsupportedFormat;

/**
 * Probe for M3U playlist.
 */
public class M3uPlaylistContainerProbe implements MediaContainerProbe {
  private static final Logger log = LoggerFactory.getLogger(M3uPlaylistContainerProbe.class);

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
  public MediaContainerDetectionResult probe(AudioReference reference, SeekableInputStream inputStream) throws IOException {
    if (!checkNextBytes(inputStream, M3U_HEADER_TAG) && !checkNextBytes(inputStream, M3U_ENTRY_TAG)) {
      return null;
    }

    log.debug("Track {} is an M3U playlist file.", reference.identifier);
    String[] lines = DataFormatTools.streamToLines(inputStream, StandardCharsets.UTF_8);

    String hlsStreamUrl = HlsStreamSegmentUrlProvider.findHlsEntryUrl(lines);

    if (hlsStreamUrl != null) {
      AudioTrackInfoBuilder infoBuilder = AudioTrackInfoBuilder.create(reference, inputStream);
      AudioReference httpReference = HttpAudioSourceManager.getAsHttpReference(reference);

      if (httpReference != null) {
        return supportedFormat(this, TYPE_HLS_OUTER, infoBuilder.setIdentifier(httpReference.identifier).build());
      } else {
        return refer(this, new AudioReference(hlsStreamUrl, infoBuilder.getTitle(),
            new MediaContainerDescriptor(this, TYPE_HLS_INNER)));
      }
    }

    MediaContainerDetectionResult result = loadSingleItemPlaylist(lines);
    if (result != null) {
      return result;
    }

    return unsupportedFormat(this, "The playlist file contains no links.");
  }

  private MediaContainerDetectionResult loadSingleItemPlaylist(String[] lines) {
    String trackTitle = null;

    for (String line : lines) {
      if (line.startsWith("#EXTINF")) {
        trackTitle = extractTitleFromInfo(line);
      } else if (!line.startsWith("#") && line.length() > 0) {
        if (line.startsWith("http://") || line.startsWith("https://") || line.startsWith("icy://")) {
          return refer(this, new AudioReference(line.trim(), trackTitle));
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
  public AudioTrack createTrack(String parameters, AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    if (parameters.equals(TYPE_HLS_INNER)) {
      return new HlsStreamTrack(trackInfo, trackInfo.identifier, httpInterfaceManager, true);
    } else if (parameters.equals(TYPE_HLS_OUTER)) {
      return new HlsStreamTrack(trackInfo, trackInfo.identifier, httpInterfaceManager, false);
    } else {
      throw new IllegalArgumentException("Unsupported parameters: " + parameters);
    }
  }
}
