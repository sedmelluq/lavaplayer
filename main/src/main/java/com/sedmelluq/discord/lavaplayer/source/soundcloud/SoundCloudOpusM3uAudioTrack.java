package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.container.ogg.OggPacketInputStream;
import com.sedmelluq.discord.lavaplayer.container.ogg.OggTrackBlueprint;
import com.sedmelluq.discord.lavaplayer.container.ogg.OggTrackHandler;
import com.sedmelluq.discord.lavaplayer.container.ogg.OggTrackLoader;
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
    try (SegmentTracker segmentTracker = createSegmentTracker()) {
      OggTrackBlueprint blueprint = OggTrackLoader.loadTrackBlueprint(segmentTracker.getOggStream());

      if (blueprint == null) {
        throw new IOException("No OGG track detected in the stream.");
      }

      localExecutor.executeProcessingLoop(() -> {
        try (OggTrackHandler handler = blueprint.loadTrackHandler(segmentTracker.getOggStream())) {
          handler.initialise(
              localExecutor.getProcessingContext(),
              segmentTracker.streamStartPosition,
              segmentTracker.desiredPosition
          );

          handler.provideFrames();
        }
      }, segmentTracker::seekToTimecode, true);
    }
  }

  private SegmentTracker createSegmentTracker() throws IOException {
    List<HlsStreamSegment> segments = HlsStreamSegmentParser.parseFromUrl(httpInterface, streamBaseUrl);
    return new SegmentTracker(segments);
  }

  private class SegmentTracker implements AutoCloseable {
    private final List<HlsStreamSegment> segments;
    private long desiredPosition = 0;
    private long streamStartPosition = 0;
    private OggPacketInputStream lastJoinedStream;
    private int segmentIndex = 0;

    private SegmentTracker(List<HlsStreamSegment> segments) {
      this.segments = segments;
    }

    private OggPacketInputStream getOggStream() {
      if (lastJoinedStream == null) {
        lastJoinedStream = new OggPacketInputStream(
            new NonSeekableInputStream(new ChainedInputStream(this::getNextStream)));
      }

      return lastJoinedStream;
    }

    private void resetStream() throws IOException {
      if (lastJoinedStream != null) {
        lastJoinedStream.close();
        lastJoinedStream = null;
      }
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
      resetStream();

      segmentIndex = index;
      desiredPosition = requestedTimecode;
      streamStartPosition = segmentTimecode;

      OggPacketInputStream nextStream = getOggStream();

      if (streamStartPosition == 0) {
        OggTrackLoader.loadTrackBlueprint(nextStream);
      } else {
        nextStream.startNewTrack();
      }
    }

    private void seekToEnd() throws IOException {
      resetStream();

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

    @Override
    public void close() throws Exception {
      resetStream();
    }
  }
}
