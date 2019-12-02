package com.sedmelluq.lavaplayer.core.container.playlists;

import com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult;
import com.sedmelluq.lavaplayer.core.container.MediaContainerHints;
import com.sedmelluq.lavaplayer.core.container.MediaContainerProbe;
import com.sedmelluq.lavaplayer.core.info.loader.AudioInfoRequests;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.tools.DataFormatTools;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetection.STREAM_SCAN_DISTANCE;
import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetection.matchNextBytesAsRegex;
import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult.refer;
import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult.unsupportedFormat;

/**
 * Probe for a playlist containing the raw link without any format.
 */
public class PlainPlaylistContainerProbe implements MediaContainerProbe {
  private static final Logger log = LoggerFactory.getLogger(PlainPlaylistContainerProbe.class);

  private static final Pattern linkPattern = Pattern.compile("^(?:https?|icy)://.*");

  @Override
  public String getName() {
    return "plain";
  }

  @Override
  public boolean matchesHints(MediaContainerHints hints) {
    return false;
  }

  @Override
  public MediaContainerDetectionResult probe(AudioInfoRequest request, SeekableInputStream inputStream) throws IOException {
    if (!matchNextBytesAsRegex(inputStream, STREAM_SCAN_DISTANCE, linkPattern, StandardCharsets.UTF_8)) {
      return null;
    }

    log.debug("Track {} is a plain playlist file.", request.name());
    return loadFromLines(request, DataFormatTools.streamToLines(inputStream, StandardCharsets.UTF_8));
  }

  private MediaContainerDetectionResult loadFromLines(AudioInfoRequest request, String[] lines) {
    for (String line : lines) {
      Matcher matcher = linkPattern.matcher(line);

      if (matcher.matches()) {
        return refer(this, AudioInfoRequests
            .genericBuilder(matcher.group(0))
            .withInheritedFields(request)
            .build());
      }
    }

    return unsupportedFormat(this, "The playlist file contains no links.");
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    throw new UnsupportedOperationException();
  }
}
