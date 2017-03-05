package com.sedmelluq.discord.lavaplayer.container.playlists;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerProbe;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.STREAM_SCAN_DISTANCE;
import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.matchNextBytesAsRegex;

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
  public MediaContainerDetectionResult probe(AudioReference reference, SeekableInputStream inputStream) throws IOException {
    if (!matchNextBytesAsRegex(inputStream, STREAM_SCAN_DISTANCE, linkPattern, StandardCharsets.UTF_8)) {
      return null;
    }

    log.debug("Track {} is a plain playlist file.", reference.identifier);
    return loadFromLines(DataFormatTools.streamToLines(inputStream, StandardCharsets.UTF_8));
  }

  private MediaContainerDetectionResult loadFromLines(String[] lines) {
    for (String line : lines) {
      Matcher matcher = linkPattern.matcher(line);

      if (matcher.matches()) {
        return new MediaContainerDetectionResult(this, new AudioReference(matcher.group(0), null));
      }
    }

    return new MediaContainerDetectionResult(this, "The playlist file contains no links.");
  }

  @Override
  public AudioTrack createTrack(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    throw new UnsupportedOperationException();
  }
}
