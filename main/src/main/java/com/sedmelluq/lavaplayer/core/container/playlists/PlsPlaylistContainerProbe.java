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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetection.checkNextBytes;
import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult.refer;
import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult.unsupportedFormat;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreTitle;

/**
 * Probe for PLS playlist.
 */
public class PlsPlaylistContainerProbe implements MediaContainerProbe {
  private static final Logger log = LoggerFactory.getLogger(PlsPlaylistContainerProbe.class);

  private static final int[] PLS_HEADER = new int[] { '[', -1, 'l', 'a', 'y', 'l', 'i', 's', 't', ']' };

  private static Pattern filePattern = Pattern.compile("\\s*File([0-9]+)=((?:https?|icy)://.*)\\s*");
  private static Pattern titlePattern = Pattern.compile("\\s*Title([0-9]+)=(.*)\\s*");

  @Override
  public String getName() {
    return "pls";
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
    if (!checkNextBytes(inputStream, PLS_HEADER)) {
      return null;
    }

    log.debug("Track {} is a PLS playlist file.", request.name());
    return loadFromLines(request, DataFormatTools.streamToLines(inputStream, StandardCharsets.UTF_8));
  }

  private MediaContainerDetectionResult loadFromLines(AudioInfoRequest request, String[] lines) {
    Map<String, String> trackFiles = new HashMap<>();
    Map<String, String> trackTitles = new HashMap<>();

    for (String line : lines) {
      Matcher fileMatcher = filePattern.matcher(line);

      if (fileMatcher.matches()) {
        trackFiles.put(fileMatcher.group(1), fileMatcher.group(2));
        continue;
      }

      Matcher titleMatcher = titlePattern.matcher(line);
      if (titleMatcher.matches()) {
        trackTitles.put(titleMatcher.group(1), titleMatcher.group(2));
      }
    }

    for (Map.Entry<String, String> entry : trackFiles.entrySet()) {
      String title = trackTitles.get(entry.getKey());

      return refer(this, AudioInfoRequests.genericBuilder(entry.getValue())
          .withInheritedFields(request)
          .withProperty(coreTitle(title))
          .build());
    }

    return unsupportedFormat(this, "The playlist file contains no links.");
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    throw new UnsupportedOperationException();
  }
}
