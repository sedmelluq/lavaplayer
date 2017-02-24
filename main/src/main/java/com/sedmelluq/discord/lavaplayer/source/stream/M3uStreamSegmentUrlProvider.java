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

public abstract class M3uStreamSegmentUrlProvider {
  protected abstract String getQualityFromM3uDirective(ExtendedM3uParser.Line directiveLine);

  protected abstract String getNextSegmentUrl(HttpInterface httpInterface);

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
    public final String quality;
    public final String url;

    private ChannelStreamInfo(String quality, String url) {
      this.quality = quality;
      this.url = url;
    }
  }
}
