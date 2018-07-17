package com.sedmelluq.discord.lavaplayer.source.stream;

import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager.createGetRequest;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;
import static com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools.fetchResponseLines;

/**
 * Provides track segment URLs for streams which use the M3U segment format. There is a base M3U containing the list of
 * different available streams. Those point to segment M3U urls, which always give the direct stream URLs of last X
 * segments. The segment provider fetches the stream for the next segment on each call to
 * {@link M3uStreamSegmentUrlProvider#getNextSegmentStream}.
 */
public abstract class M3uStreamSegmentUrlProvider {
  private static final long SEGMENT_WAIT_STEP_MS = 200;

  protected SegmentInfo lastSegment;

  protected static String createSegmentUrl(String playlistUrl, String segmentName) {
    return URI.create(playlistUrl).resolve(segmentName).toString();
  }

  /**
   * If applicable, extracts the quality information from the M3U directive which describes one stream in the root M3U.
   *
   * @param directiveLine Directive line with arguments.
   * @return The quality name extracted from the directive line.
   */
  protected abstract String getQualityFromM3uDirective(ExtendedM3uParser.Line directiveLine);

  protected abstract String fetchSegmentPlaylistUrl(HttpInterface httpInterface) throws IOException;

  /**
   * Logic for getting the URL for the next segment.
   *
   * @param httpInterface HTTP interface to use for any requests required to perform to find the segment URL.
   * @return The direct stream URL of the next segment.
   */
  protected String getNextSegmentUrl(HttpInterface httpInterface) {
    try {
      String streamSegmentPlaylistUrl = fetchSegmentPlaylistUrl(httpInterface);
      if (streamSegmentPlaylistUrl == null) {
        return null;
      }

      long startTime = System.currentTimeMillis();
      SegmentInfo nextSegment;

      while (true) {
        List<SegmentInfo> segments = loadStreamSegmentsList(httpInterface, streamSegmentPlaylistUrl);
        nextSegment = chooseNextSegment(segments, lastSegment);

        if (nextSegment != null || !shouldWaitForSegment(startTime, segments)) {
          break;
        }

        Thread.sleep(SEGMENT_WAIT_STEP_MS);
      }

      if (nextSegment == null) {
        return null;
      }

      lastSegment = nextSegment;
      return createSegmentUrl(streamSegmentPlaylistUrl, lastSegment.url);
    } catch (IOException e) {
      throw new FriendlyException("Failed to get next part of the stream.", SUSPICIOUS, e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Fetches the input stream for the next segment in the M3U stream.
   *
   * @param httpInterface HTTP interface to use for any requests required to perform to find the segment URL.
   * @return Input stream of the next segment.
   */
  public InputStream getNextSegmentStream(HttpInterface httpInterface) {
    String url = getNextSegmentUrl(httpInterface);
    if (url == null) {
      return null;
    }

    CloseableHttpResponse response = null;
    boolean success = false;

    try {
      response = httpInterface.execute(createGetRequest(url));
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode != 200) {
        throw new IOException("Invalid status code from segment data URL: " + statusCode);
      }

      success = true;
      return response.getEntity().getContent();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (response != null && !success) {
        ExceptionTools.closeWithWarnings(response);
      }
    }
  }

  protected List<ChannelStreamInfo> loadChannelStreamsList(String[] lines) {
    ExtendedM3uParser.Line streamInfoLine = null;

    List<ChannelStreamInfo> streams = new ArrayList<>();

    for (String lineText : lines) {
      ExtendedM3uParser.Line line = ExtendedM3uParser.parseLine(lineText);

      if (line.isData() && streamInfoLine != null) {
        String quality = getQualityFromM3uDirective(streamInfoLine);
        if (quality != null) {
          streams.add(new ChannelStreamInfo(quality, line.lineData));
        }

        streamInfoLine = null;
      } else if (line.isDirective() && "EXT-X-STREAM-INF".equals(line.directiveName)) {
        streamInfoLine = line;
      }
    }

    return streams;
  }

  protected List<SegmentInfo> loadStreamSegmentsList(HttpInterface httpInterface, String streamSegmentPlaylistUrl) throws IOException {
    List<SegmentInfo> segments = new ArrayList<>();
    ExtendedM3uParser.Line segmentInfo = null;

    for (String lineText : fetchResponseLines(httpInterface, new HttpGet(streamSegmentPlaylistUrl), "stream segments list")) {
      ExtendedM3uParser.Line line = ExtendedM3uParser.parseLine(lineText);

      if (line.isDirective() && "EXTINF".equals(line.directiveName)) {
        segmentInfo = line;
      }

      if (line.isData()) {
        if (segmentInfo != null && segmentInfo.extraData.contains(",")) {
          String[] fields = segmentInfo.extraData.split(",", 2);
          segments.add(new SegmentInfo(line.lineData, parseSecondDuration(fields[0]), fields[1]));
        } else {
          segments.add(new SegmentInfo(line.lineData, null, null));
        }
      }
    }

    return segments;
  }

  private static Long parseSecondDuration(String value) {
    try {
      double asDouble = Double.parseDouble(value);
      return (long) (asDouble * 1000.0);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  protected SegmentInfo chooseNextSegment(List<SegmentInfo> segments, SegmentInfo lastSegment) {
    SegmentInfo selected = null;

    for (int i = segments.size() - 1; i >= 0; i--) {
      SegmentInfo current = segments.get(i);
      if (lastSegment != null && current.url.equals(lastSegment.url)) {
        break;
      }

      selected = current;
    }

    return selected;
  }

  private boolean shouldWaitForSegment(long startTime, List<SegmentInfo> segments) {
    if (!segments.isEmpty()) {
      SegmentInfo sampleSegment = segments.get(0);

      if (sampleSegment.duration != null) {
        return System.currentTimeMillis() - startTime < sampleSegment.duration;
      }
    }

    return false;
  }

  protected static class ChannelStreamInfo {
    /**
     * Stream quality extracted from stream M3U directive.
     */
    public final String quality;
    /**
     * URL for stream segment list.
     */
    public final String url;

    private ChannelStreamInfo(String quality, String url) {
      this.quality = quality;
      this.url = url;
    }
  }

  protected static class SegmentInfo {
    /**
     * URL of the segment.
     */
    public final String url;
    /**
     * Duration of the segment in milliseconds. <code>null</code> if unknown.
     */
    public final Long duration;
    /**
     * Name of the segment. <code>null</code> if unknown.
     */
    public final String name;

    public SegmentInfo(String url, Long duration, String name) {
      this.url = url;
      this.duration = duration;
      this.name = name;
    }
  }
}
