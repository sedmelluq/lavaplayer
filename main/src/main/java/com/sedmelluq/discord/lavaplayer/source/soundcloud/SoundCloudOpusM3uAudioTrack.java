package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.container.ogg.OggAudioTrack;
import com.sedmelluq.discord.lavaplayer.container.ogg.OggTrackPosition;
import com.sedmelluq.discord.lavaplayer.container.playlists.HlsStreamSegment;
import com.sedmelluq.discord.lavaplayer.container.playlists.HlsStreamSegmentParser;
import com.sedmelluq.discord.lavaplayer.tools.http.HttpStreamTools;
import com.sedmelluq.discord.lavaplayer.tools.io.ChainedInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.NonSeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.http.client.methods.HttpGet;

public class SoundCloudOpusM3uAudioTrack extends DelegatedAudioTrack {
  private final HttpInterface httpInterface;
  private final String streamBaseUrl;

  public SoundCloudOpusM3uAudioTrack(AudioTrackInfo trackInfo, HttpInterface httpInterface, String streamBaseUrl) {
    super(trackInfo);
    this.httpInterface = httpInterface;
    this.streamBaseUrl = streamBaseUrl;
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
    SegmentTracker segmentTracker = createSegmentTracker();

    localExecutor.executeProcessingLoop(() -> {
      try (ChainedInputStream chained = new ChainedInputStream(segmentTracker::getNextStream)) {
        processDelegate(new OggAudioTrack(
            trackInfo,
            new NonSeekableInputStream(chained),
            segmentTracker.track
        ), localExecutor);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }, segmentTracker::seekToTimecode, true);
  }

  private SegmentTracker createSegmentTracker() throws IOException {
    List<HlsStreamSegment> segments = HlsStreamSegmentParser.parseFromUrl(httpInterface, streamBaseUrl);
    return new SegmentTracker(segments);
  }

  private class SegmentTracker {
    private final List<HlsStreamSegment> segments;
    private final OggAudioTrack.Persistent track = new OggAudioTrack.Persistent();
    private int segmentIndex = 0;

    private SegmentTracker(List<HlsStreamSegment> segments) {
      this.segments = segments;
    }

    private void seekToTimecode(long timecode) {
      long segmentTimecode = 0;

      for (int i = 0; i < segments.size(); i++) {
        Long duration = segments.get(i).duration;

        if (duration == null) {
          break;
        }

        long nextTimecode = segmentTimecode + duration;

        if (timecode >= segmentTimecode && timecode < nextTimecode) {
          segmentIndex = i;
          track.position = new OggTrackPosition(timecode, segmentTimecode);
          return;
        }

        segmentTimecode = nextTimecode;
      }

      segmentIndex = segments.size();
    }

    private InputStream getNextStream() {
      HlsStreamSegment segment = getNextSegment();

      if (segment == null) {
        return null;
      }

      return HttpStreamTools.streamContent(httpInterface, new HttpGet(segment.url));
    }

    private HlsStreamSegment getNextSegment() {
      int current = segmentIndex++;

      if (current < segments.size()) {
        return segments.get(current);
      } else {
        return null;
      }
    }
  }
}
