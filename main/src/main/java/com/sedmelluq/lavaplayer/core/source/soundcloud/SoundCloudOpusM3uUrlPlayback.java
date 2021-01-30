package com.sedmelluq.lavaplayer.core.source.soundcloud;

import com.sedmelluq.lavaplayer.core.container.playlists.HlsStreamSegment;
import com.sedmelluq.lavaplayer.core.container.playlists.HlsStreamSegmentParser;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.HttpStreamTools;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.io.ChainedInputStream;
import com.sedmelluq.lavaplayer.core.tools.io.NonSeekableInputStream;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoundCloudOpusM3uUrlPlayback implements AudioPlayback {
  private static final Logger log = LoggerFactory.getLogger(SoundCloudOpusM3uUrlPlayback.class);

  private static final long SEGMENT_UPDATE_INTERVAL = TimeUnit.MINUTES.toMillis(10);

  private final String identifier;
  private final HttpInterface httpInterface;
  private final SoundCloudM3uInfo m3uInfo;

  public SoundCloudOpusM3uUrlPlayback(String identifier, HttpInterface httpInterface, SoundCloudM3uInfo m3uInfo) {
    this.identifier = identifier;
    this.httpInterface = httpInterface;
    this.m3uInfo = m3uInfo;
  }

  @Override
  public void process(AudioPlaybackController controller) {
    try (SegmentTracker segmentTracker = createSegmentTracker()) {
      segmentTracker.decoder.prepareStream(true);

      controller.executeProcessingLoop(() -> segmentTracker.decoder.playStream(
          controller.getContext(),
          segmentTracker.streamStartPosition,
          segmentTracker.desiredPosition
      ), segmentTracker::seekToTimecode, true);
    } catch (Exception e) {
      throw ExceptionTools.toRuntimeException(e);
    }
  }

  private List<HlsStreamSegment> loadSegments() throws IOException {
    String playbackUrl = SoundCloudHelper.loadPlaybackUrl(httpInterface, m3uInfo.lookupUrl);
    return HlsStreamSegmentParser.parseFromUrl(httpInterface, playbackUrl);
  }

  private SegmentTracker createSegmentTracker() throws IOException {
    List<HlsStreamSegment> initialSegments = loadSegments();
    SegmentTracker tracker = new SegmentTracker(initialSegments);
    tracker.setupDecoder(m3uInfo.decoderFactory);
    return tracker;
  }

  private class SegmentTracker implements AutoCloseable {
    private final List<HlsStreamSegment> segments;
    private long desiredPosition = 0;
    private long streamStartPosition = 0;
    private long lastUpdate;
    private SoundCloudSegmentDecoder decoder;
    private int segmentIndex = 0;

    private SegmentTracker(List<HlsStreamSegment> segments) {
      this.segments = segments;
      this.lastUpdate = System.currentTimeMillis();
    }

    private void setupDecoder(SoundCloudSegmentDecoder.Factory factory) {
      decoder = factory.create(this::createChainedStream);
    }

    private SeekableInputStream createChainedStream() {
      return new NonSeekableInputStream(new ChainedInputStream(this::getNextStream));
    }

    private void seekToTimecode(long timecode) throws IOException {
      long segmentTimecode = 0;

      for (int i = 0; i < segments.size(); i++) {
        Long duration = segments.get(i).duration;

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

      segmentIndex = segments.size();
    }

    private InputStream getNextStream() {
      HlsStreamSegment segment = getNextSegment();

      if (segment == null) {
        return null;
      }

      return HttpStreamTools.streamContent(httpInterface, new HttpGet(segment.url));
    }

    private void updateSegmentList() {
      try {
        List<HlsStreamSegment> newSegments = loadSegments();

        if (newSegments.size() != segments.size()) {
          log.error("For {}, received different number of segments on update, skipping.", identifier);
          return;
        }

        for (int i = 0; i < segments.size(); i++) {
          if (!Objects.equals(newSegments.get(i).duration, segments.get(i).duration)) {
            log.error("For {}, segment {} has different length than previously on update.", identifier, i);
            return;
          }
        }

        for (int i = 0; i < segments.size(); i++) {
          segments.set(i, newSegments.get(i));
        }
      } catch (Exception e) {
        log.error("For {}, failed to update segment list, skipping.", identifier, e);
      }
    }

    private void checkSegmentListUpdate() {
      long now = System.currentTimeMillis();
      long delta = now - lastUpdate;

      if (delta > SEGMENT_UPDATE_INTERVAL) {
        log.debug("For {}, {}ms has passed since last segment update, updating", identifier, delta);

        updateSegmentList();
        lastUpdate = now;
      }
    }

    private HlsStreamSegment getNextSegment() {
      int current = segmentIndex++;

      if (current < segments.size()) {
        checkSegmentListUpdate();
        return segments.get(current);
      } else {
        return null;
      }
    }

    @Override
    public void close() throws Exception {
      decoder.resetStream();
    }
  }
}
