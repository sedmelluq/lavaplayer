package com.sedmelluq.discord.lavaplayer.source.twitch;

import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager.createGetRequest;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Provider for Twitch segment URLs from a channel.
 */
public class TwitchStreamSegmentUrlProvider {
  private static final String TOKEN_PARAMETER = "token";

  private static final Logger log = LoggerFactory.getLogger(TwitchStreamSegmentUrlProvider.class);

  private final String channelName;
  private String streamSegmentPlaylistUrl;
  private String lastSegment;
  private long tokenExpirationTime;

  /**
   * @param channelName Channel identifier.
   */
  public TwitchStreamSegmentUrlProvider(String channelName) {
    this.channelName = channelName;
    this.tokenExpirationTime = -1;
  }

  /**
   * @param httpInterface Http interface to use for requests.
   * @return The URL of the next TS segment.
   */
  public String getNextSegmentUrl(HttpInterface httpInterface) {
    try {
      if (!obtainSegmentPlaylistUrl(httpInterface)) {
        return null;
      }

      List<String> segments = loadStreamSegmentsList(httpInterface);
      String segment = chooseNextSegment(segments);

      if (segment == null) {
        return null;
      }

      return createSegmentUrl(streamSegmentPlaylistUrl, segment);
    } catch (IOException e) {
      throw new FriendlyException("Failed to get next part of the stream.", SUSPICIOUS, e);
    }
  }

  private List<String> loadStreamSegmentsList(HttpInterface httpInterface) throws IOException {
    List<String> segments = new ArrayList<>();

    for (String lineText : getLinesFromUrl(httpInterface, streamSegmentPlaylistUrl, "stream segments list")) {
      ExtendedM3uParser.Line line = ExtendedM3uParser.parseLine(lineText);

      if (line.isData()) {
        segments.add(line.lineData);
      }
    }

    return segments;
  }

  private String chooseNextSegment(List<String> segments) {
    String selected = null;

    for (int i = segments.size() - 1; i >= 0; i--) {
      String current = segments.get(i);
      if (current.equals(lastSegment)) {
        break;
      }

      selected = current;
    }

    if (selected != null) {
      lastSegment = selected;
    }

    return selected;
  }

  private static String createSegmentUrl(String playlistUrl, String segmentName) {
    return playlistUrl.substring(0, playlistUrl.lastIndexOf('/') + 1) + segmentName;
  }

  private boolean obtainSegmentPlaylistUrl(HttpInterface httpInterface) throws IOException {
    if (System.currentTimeMillis() < tokenExpirationTime) {
      return true;
    }

    JsonBrowser token = loadAccessToken(httpInterface);
    String channelStreamsUrl = getChannelStreamsUrl(token).toString();
    ChannelStreams streams = loadChannelStreamsList(getLinesFromUrl(httpInterface, channelStreamsUrl, "channel streams list"));

    if (streams.entries.isEmpty()) {
      throw new IllegalStateException("No streams available on channel.");
    }

    ChannelStreamInfo stream = streams.entries.get(0);

    log.debug("Chose stream with quality {} from url {}", stream.quality, stream.url);
    streamSegmentPlaylistUrl = stream.url;

    long tokenServerExpirationTime = JsonBrowser.parse(token.get(TOKEN_PARAMETER).text()).get("expires").as(Long.class) * 1000L;
    tokenExpirationTime = System.currentTimeMillis() + (tokenServerExpirationTime - streams.serverTime) - 5000;

    return true;
  }

  private JsonBrowser loadAccessToken(HttpInterface httpInterface) throws IOException {
    HttpUriRequest request = createGetRequest("https://api.twitch.tv/api/channels/" + channelName +
        "/access_token?adblock=false&need_https=true&platform=web&player_type=site");

    try (CloseableHttpResponse response = httpInterface.execute(request)) {
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != 200) {
        throw new IOException("Unexpected response code from access token request: " + statusCode);
      }

      return JsonBrowser.parse(response.getEntity().getContent());
    }
  }

  private ChannelStreams loadChannelStreamsList(String[] lines) throws IOException {
    ExtendedM3uParser.Line twitchInfoLine = null;
    ExtendedM3uParser.Line streamInfoLine = null;

    List<ChannelStreamInfo> streams = new ArrayList<>();

    for (String lineText : lines) {
      ExtendedM3uParser.Line line = ExtendedM3uParser.parseLine(lineText);

      if (line.isData() && streamInfoLine != null) {
        String quality = streamInfoLine.directiveArguments.get("VIDEO");

        if (quality != null) {
          streams.add(new ChannelStreamInfo(quality, line.lineData));
        }

        streamInfoLine = null;
      } else if (line.isDirective()) {
        if ("EXT-X-TWITCH-INFO".equals(line.directiveName)) {
          twitchInfoLine = line;
        } else if ("EXT-X-STREAM-INF".equals(line.directiveName)) {
          streamInfoLine = line;
        }
      }
    }

    return buildChannelStreamsList(twitchInfoLine, streams);
  }

  private ChannelStreams buildChannelStreamsList(ExtendedM3uParser.Line twitchInfoLine, List<ChannelStreamInfo> streams) {
    String serverTimeValue = twitchInfoLine != null ? twitchInfoLine.directiveArguments.get("SERVER-TIME") : null;

    if (serverTimeValue == null) {
      throw new IllegalStateException("Required server time information not available.");
    }

    return new ChannelStreams(
        (long) (Double.valueOf(serverTimeValue) * 1000.0),
        streams
    );
  }

  private String[] getLinesFromUrl(HttpInterface httpInterface, String url, String name) throws IOException {
    HttpUriRequest request = createGetRequest(url);

    try (CloseableHttpResponse response = httpInterface.execute(request)) {
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != 200) {
        throw new IOException("Unexpected response code " + statusCode + " from " + name);
      }

      return DataFormatTools.streamToLines(response.getEntity().getContent(), StandardCharsets.UTF_8);
    }
  }

  private URI getChannelStreamsUrl(JsonBrowser token) {
    try {
      return new URIBuilder("https://usher.ttvnw.net/api/channel/hls/" + channelName + ".m3u8")
          .addParameter("token", token.get(TOKEN_PARAMETER).text())
          .addParameter("sig", token.get("sig").text())
          .addParameter("allow_source", "true")
          .addParameter("allow_spectre", "true")
          .addParameter("player_backend", "html5")
          .addParameter("expgroup", "regular")
          .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private static class ChannelStreams {
    private final long serverTime;
    private final List<ChannelStreamInfo> entries;

    private ChannelStreams(long serverTime, List<ChannelStreamInfo> entries) {
      this.serverTime = serverTime;
      this.entries = entries;
    }
  }

  private static class ChannelStreamInfo {
    private final String quality;
    private final String url;

    private ChannelStreamInfo(String quality, String url) {
      this.quality = quality;
      this.url = url;
    }
  }
}
