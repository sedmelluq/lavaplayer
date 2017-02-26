package com.sedmelluq.discord.lavaplayer.source.stream;

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager.createGetRequest;
import static com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools.fetchResponseLines;

/**
 * Provides track segment URLs for streams which use the M3U segment format. There is a base M3U containing the list of
 * different available streams. Those point to segment M3U urls, which always give the direct stream URLs of last X
 * segments. The segment provider fetches the stream for the next segment on each call to
 * {@link M3uStreamSegmentUrlProvider#getNextSegmentStream}.
 */
public abstract class M3uStreamSegmentUrlProvider {
  /**
   * If applicable, extracts the quality information from the M3U directive which describes one stream in the root M3U.
   *
   * @param directiveLine Directive line with arguments.
   * @return The quality name extracted from the directive line.
   */
  protected abstract String getQualityFromM3uDirective(ExtendedM3uParser.Line directiveLine);

  /**
   * Implementation specific logic for getting the URL for the next segment.
   *
   * @param httpInterface HTTP interface to use for any requests required to perform to find the segment URL.
   * @return The direct stream URL of the next segment.
   */
  protected abstract String getNextSegmentUrl(HttpInterface httpInterface);

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
        IOUtils.closeQuietly(response);
      }
    }
  }

  protected List<ChannelStreamInfo> loadChannelStreamsList(String[] lines) throws IOException {
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
      } else if (line.isDirective()) {
        if ("EXT-X-STREAM-INF".equals(line.directiveName)) {
          streamInfoLine = line;
        }
      }
    }

    return streams;
  }

  protected List<String> loadStreamSegmentsList(HttpInterface httpInterface, String streamSegmentPlaylistUrl) throws IOException {
    List<String> segments = new ArrayList<>();

    for (String lineText : fetchResponseLines(httpInterface, new HttpGet(streamSegmentPlaylistUrl), "stream segments list")) {
      ExtendedM3uParser.Line line = ExtendedM3uParser.parseLine(lineText);

      if (line.isData()) {
        segments.add(line.lineData);
      }
    }

    return segments;
  }

  protected String chooseNextSegment(List<String> segments, String lastSegment) {
    String selected = null;

    for (int i = segments.size() - 1; i >= 0; i--) {
      String current = segments.get(i);
      if (current.equals(lastSegment)) {
        break;
      }

      selected = current;
    }

    return selected;
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
}
