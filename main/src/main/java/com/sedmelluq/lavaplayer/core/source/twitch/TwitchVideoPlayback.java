package com.sedmelluq.lavaplayer.core.source.twitch;

import com.sedmelluq.lavaplayer.core.container.playlists.ExtendedM3uParser;
import com.sedmelluq.lavaplayer.core.container.playlists.HlsStreamSegment;
import com.sedmelluq.lavaplayer.core.container.playlists.HlsStreamSegmentParser;
import com.sedmelluq.lavaplayer.core.http.HttpClientTools;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.HttpInterfaceManager;
import com.sedmelluq.lavaplayer.core.http.HttpStreamTools;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import com.sedmelluq.lavaplayer.core.source.soundcloud.SoundCloudSegmentDecoder;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.io.ChainedInputStream;
import com.sedmelluq.lavaplayer.core.tools.io.NonSeekableInputStream;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;

public class TwitchVideoPlayback implements AudioPlayback {
  private final HttpInterfaceManager interfaceManager;
  private final TwitchVideoPlaybackInfo playbackInfo;

  public TwitchVideoPlayback(HttpInterfaceManager interfaceManager, TwitchVideoPlaybackInfo playbackInfo) {
    this.interfaceManager = interfaceManager;
    this.playbackInfo = playbackInfo;
  }

  @Override
  public void process(AudioPlaybackController controller) {
    try (HttpInterface httpInterface = interfaceManager.getInterface()) {
      try (SegmentTracker segmentTracker = createSegmentTracker(httpInterface)) {
        segmentTracker.decoder.prepareStream(true);

        controller.executeProcessingLoop(() -> segmentTracker.decoder.playStream(
            controller.getContext(),
            segmentTracker.streamStartPosition,
            segmentTracker.desiredPosition
        ), segmentTracker::seekToTimecode, true);
      }
    } catch (Exception e) {
      throw ExceptionTools.toRuntimeException(e);
    }
  }

  private String fetchSegmentListUrl(HttpInterface httpInterface) throws Exception {
    URI url = new URIBuilder("https://usher.ttvnw.net/vod/" + playbackInfo.videoId + ".m3u8")
        .addParameter("allow_source", "true")
        .addParameter("p", String.valueOf(ThreadLocalRandom.current().nextInt(100000000)))
        .addParameter("play_session_id", "22222222222222222222222222222222")
        .addParameter("player_backend", "mediaplayer")
        .addParameter("playlist_include_framerate", "true")
        .addParameter("reassignments_supported", "true")
        .addParameter("sig", playbackInfo.signature)
        .addParameter("supported_codecs", "vp09,avc1")
        .addParameter("token", playbackInfo.token)
        .addParameter("cdm", "vw")
        .addParameter("player_version", "1.2.0")
        .build();

    String[] lines = HttpClientTools.fetchResponseLines(httpInterface, new HttpGet(url), "channel list");

    ExtendedM3uParser.Line streamInfoLine = null;
    String latestUrl = null;
    long latestUrlBandwidth = 0;

    for (String lineText : lines) {
      ExtendedM3uParser.Line line = ExtendedM3uParser.parseLine(lineText);

      if (line.isData() && streamInfoLine != null) {
        String bandwidthArgument = streamInfoLine.directiveArguments.get("BANDWIDTH");
        long bandwidth = bandwidthArgument == null ? 0 : Long.parseLong(bandwidthArgument);

        if (latestUrl == null || bandwidth > latestUrlBandwidth) {
          latestUrl = line.lineData;
          latestUrlBandwidth = bandwidth;
        }

        streamInfoLine = null;
      } else if (line.isDirective() && "EXT-X-STREAM-INF".equals(line.directiveName)) {
        streamInfoLine = line;
      }
    }

    return latestUrl;
  }

  private SegmentList loadSegments(HttpInterface httpInterface) {
    try {
      String segmentListUrl = fetchSegmentListUrl(httpInterface);

      List<HlsStreamSegment> segments = HlsStreamSegmentParser.parseFromUrl(httpInterface, segmentListUrl);
      String urlPrefix = segmentListUrl.substring(0, segmentListUrl.lastIndexOf('/') + 1);

      return new SegmentList(urlPrefix, segments);
    } catch (Exception e) {
      throw ExceptionTools.toRuntimeException(e);
    }
  }

  private SegmentTracker createSegmentTracker(HttpInterface httpInterface) {
    SegmentList initialSegments = loadSegments(httpInterface);
    SegmentTracker tracker = new SegmentTracker(initialSegments, httpInterface);
    tracker.setupDecoder(TwitchMpegSegmentDecoder::new);
    return tracker;
  }

  private static class SegmentTracker implements AutoCloseable {
    private final SegmentList segmentList;
    private final HttpInterface httpInterface;
    private long desiredPosition = 0;
    private long streamStartPosition = 0;
    private SoundCloudSegmentDecoder decoder;
    private int segmentIndex = 0;

    private SegmentTracker(SegmentList segmentList, HttpInterface httpInterface) {
      this.segmentList = segmentList;
      this.httpInterface = httpInterface;
    }

    private void setupDecoder(SoundCloudSegmentDecoder.Factory factory) {
      decoder = factory.create(this::createChainedStream);
    }

    private SeekableInputStream createChainedStream() {
      return new NonSeekableInputStream(new ChainedInputStream(this::getNextStream));
    }

    private void seekToTimecode(long timecode) throws IOException {
      long segmentTimecode = 0;

      for (int i = 0; i < segmentList.segments.size(); i++) {
        Long duration = segmentList.segments.get(i).duration;

        if (duration == null) {
          break;
        }

        long nextTimecode = segmentTimecode + duration;

        if (timecode >= segmentTimecode && timecode < nextTimecode) {
          seekToSegment(i, timecode, segmentTimecode);
          return;
        }

        segmentTimecode = nextTimecode;
      }

      seekToEnd();
    }

    private void seekToSegment(int index, long requestedTimecode, long segmentTimecode) throws IOException {
      decoder.resetStream();

      segmentIndex = index;
      desiredPosition = requestedTimecode;
      streamStartPosition = segmentTimecode;

      decoder.prepareStream(streamStartPosition == 0);
    }

    private void seekToEnd() throws IOException {
      decoder.resetStream();

      segmentIndex = segmentList.segments.size();
    }

    private InputStream getNextStream() {
      HlsStreamSegment segment = getNextSegment();

      if (segment == null) {
        return null;
      }

      return HttpStreamTools.streamContent(httpInterface, new HttpGet(segmentList.urlPrefix + segment.url));
    }

    private HlsStreamSegment getNextSegment() {
      int current = segmentIndex++;

      if (current < segmentList.segments.size()) {
        return segmentList.segments.get(current);
      } else {
        return null;
      }
    }

    @Override
    public void close() throws Exception {
      decoder.resetStream();
    }
  }

  private static class SegmentList {
    private final String urlPrefix;
    private final List<HlsStreamSegment> segments;

    private SegmentList(String urlPrefix, List<HlsStreamSegment> segments) {
      this.urlPrefix = urlPrefix;
      this.segments = segments;
    }
  }
}
