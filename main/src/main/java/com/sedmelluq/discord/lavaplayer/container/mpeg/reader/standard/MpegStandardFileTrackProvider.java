package com.sedmelluq.discord.lavaplayer.container.mpeg.reader.standard;

import com.sedmelluq.discord.lavaplayer.container.mpeg.reader.MpegFileTrackProvider;
import com.sedmelluq.discord.lavaplayer.container.mpeg.reader.MpegReader;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegTrackConsumer;
import com.sedmelluq.discord.lavaplayer.container.mpeg.reader.MpegVersionedSectionInfo;
import com.sedmelluq.discord.lavaplayer.tools.io.DetachedByteChannel;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Track provider for the standard (non-fragmented) MP4 file format.
 */
public class MpegStandardFileTrackProvider implements MpegFileTrackProvider {
  private final MpegReader reader;
  private final List<TrackSeekInfoBuilder> builders = new ArrayList<>();
  private final Map<Integer, Integer> trackTimescales = new HashMap<>();
  private int timescale;
  private int currentChunk;
  private MpegTrackConsumer consumer;
  private TrackSeekInfo seekInfo;

  /**
   * @param reader MP4-specific reader
   */
  public MpegStandardFileTrackProvider(MpegReader reader) {
    this.reader = reader;
    this.currentChunk = 0;
  }

  @Override
  public boolean initialise(MpegTrackConsumer consumer) {
    this.consumer = consumer;

    int trackId = consumer.getTrack().trackId;

    if (!trackTimescales.containsKey(trackId)) {
      return false;
    }

    try {
      for (TrackSeekInfoBuilder builder : builders) {
        if (builder.trackId == trackId) {
          seekInfo = builder.build();
          timescale = trackTimescales.get(trackId);
          return true;
        }
      }
    } finally {
      builders.clear();
    }

    return false;
  }

  @Override
  public long getDuration() {
    return seekInfo.totalDuration * 1000L / timescale;
  }

  @Override
  public void provideFrames() throws InterruptedException {
    try (ReadableByteChannel channel = new DetachedByteChannel(Channels.newChannel(reader.seek))) {
      while (currentChunk < seekInfo.chunkOffsets.length) {
        reader.seek.seek(seekInfo.chunkOffsets[currentChunk]);

        int[] samples = seekInfo.chunkSamples[currentChunk];
        for (int i = 0; i < samples.length; i++) {
          consumer.consume(channel, samples[i]);
        }

        currentChunk++;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void seekToTimecode(long timecode) {
    long scaledTimecode = timecode * timescale / 1000;
    int length = seekInfo.chunkOffsets.length;

    if (scaledTimecode >= seekInfo.totalDuration) {
      currentChunk = length;
      consumer.seekPerformed(timecode, seekInfo.totalDuration * 1000 / timescale);
    } else {
      for (int i = 0; i < length; i++) {
        long nextTimecode = i < length - 1 ? seekInfo.chunkTimecodes[i + 1] : seekInfo.totalDuration;

        if (scaledTimecode < nextTimecode) {
          consumer.seekPerformed(timecode, seekInfo.chunkTimecodes[i] * 1000 / timescale);
          currentChunk = i;
          break;
        }
      }
    }
  }

  /**
   * Read the mdhd section for a track.
   * @param mdhd The section header
   * @param trackId Track ID
   * @throws IOException On read error.
   */
  public void readMediaHeaders(MpegVersionedSectionInfo mdhd, int trackId) throws IOException {
    int trackTimescale;

    if (mdhd.version == 1) {
      reader.data.readLong(); // creation time
      reader.data.readLong(); // modification time
      trackTimescale = reader.data.readInt();
      reader.data.readLong(); // duration
    } else {
      reader.data.readInt(); // creation time
      reader.data.readInt(); // modification time
      trackTimescale = reader.data.readInt();
      reader.data.readInt(); // duration
    }

    trackTimescales.put(trackId, trackTimescale);
  }

  /**
   * Attaches standard format specific handlers to sample table section handle chain.
   * @param sampleTableChain Sample table child section handler chain.
   * @param trackId Track ID
   */
  public void attachSampleTableParsers(MpegReader.Chain sampleTableChain, int trackId) {
    final TrackSeekInfoBuilder seekInfoBuilder = new TrackSeekInfoBuilder(trackId);

    sampleTableChain
        .handleVersioned("stts", stts -> parseTimeToSample(seekInfoBuilder))
        .handleVersioned("stsc", stsc -> parseSampleToChunk(seekInfoBuilder))
        .handleVersioned("stsz", stsz -> parseSampleSizes(seekInfoBuilder))
        .handleVersioned("stco", stco -> parseChunkOffsets32(seekInfoBuilder))
        .handleVersioned("co64", co64 -> parseChunkOffsets64(seekInfoBuilder));

    builders.add(seekInfoBuilder);
  }

  private void parseTimeToSample(TrackSeekInfoBuilder seekInfoBuilder) throws IOException {
    int entries = reader.data.readInt();
    seekInfoBuilder.sampleTimeCounts = new int[entries];
    seekInfoBuilder.sampleTimeDeltas = new int[entries];
    seekInfoBuilder.presence |= 1;

    for (int i = 0; i < entries; i++) {
      seekInfoBuilder.sampleTimeCounts[i] = reader.data.readInt();
      seekInfoBuilder.sampleTimeDeltas[i] = reader.data.readInt();
    }
  }

  private void parseSampleToChunk(TrackSeekInfoBuilder seekInfoBuilder) throws IOException {
    int entries = reader.data.readInt();
    seekInfoBuilder.sampleChunkingFirst = new int[entries];
    seekInfoBuilder.sampleChunkingCount = new int[entries];
    seekInfoBuilder.presence |= 2;

    for (int i = 0; i < entries; i++) {
      seekInfoBuilder.sampleChunkingFirst[i] = reader.data.readInt();
      seekInfoBuilder.sampleChunkingCount[i] = reader.data.readInt();
      reader.data.readInt();
    }
  }

  private void parseSampleSizes(TrackSeekInfoBuilder seekInfoBuilder) throws IOException {
    seekInfoBuilder.sampleSize = reader.data.readInt();
    seekInfoBuilder.sampleCount = reader.data.readInt();
    seekInfoBuilder.presence |= 4;

    if (seekInfoBuilder.sampleSize == 0) {
      seekInfoBuilder.sampleSizes = new int[seekInfoBuilder.sampleCount];

      for (int i = 0; i < seekInfoBuilder.sampleCount; i++) {
        seekInfoBuilder.sampleSizes[i] = reader.data.readInt();
      }
    }
  }

  private void parseChunkOffsets32(TrackSeekInfoBuilder seekInfoBuilder) throws IOException {
    int chunks = reader.data.readInt();
    seekInfoBuilder.chunkOffsets = new long[chunks];
    seekInfoBuilder.presence |= 8;

    for (int i = 0; i < chunks; i++) {
      seekInfoBuilder.chunkOffsets[i] = reader.data.readInt();
    }
  }

  private void parseChunkOffsets64(TrackSeekInfoBuilder seekInfoBuilder) throws IOException {
    int chunks = reader.data.readInt();
    seekInfoBuilder.chunkOffsets = new long[chunks];
    seekInfoBuilder.presence |= 8;

    for (int i = 0; i < chunks; i++) {
      seekInfoBuilder.chunkOffsets[i] = reader.data.readLong();
    }
  }

  private static class TrackSeekInfo {
    private final long totalDuration;
    private final long[] chunkOffsets;
    private final long[] chunkTimecodes;
    private final int[][] chunkSamples;

    private TrackSeekInfo(long totalDuration, long[] chunkOffsets, long[] chunkTimecodes, int[][] chunkSamples) {
      this.totalDuration = totalDuration;
      this.chunkOffsets = chunkOffsets;
      this.chunkTimecodes = chunkTimecodes;
      this.chunkSamples = chunkSamples;
    }
  }

  private static class TrackSeekInfoBuilder {
    private final int trackId;
    private int presence;
    private int[] sampleTimeCounts;
    private int[] sampleTimeDeltas;
    private int[] sampleChunkingFirst;
    private int[] sampleChunkingCount;
    private long[] chunkOffsets;
    private int sampleSize;
    private int sampleCount;
    private int[] sampleSizes;

    private TrackSeekInfoBuilder(int trackId) {
      this.trackId = trackId;
    }

    private TrackSeekInfo build() {
      if (presence != 15) {
        return null;
      }

      long[] chunkTimecodes = new long[chunkOffsets.length];
      int[][] chunkSamples = new int[chunkOffsets.length][];

      SampleChunkingIterator chunkingIterator = new SampleChunkingIterator(sampleChunkingFirst, sampleChunkingCount);
      SampleDurationIterator durationIterator = new SampleDurationIterator(sampleTimeCounts, sampleTimeDeltas);

      int sampleOffset = 0;
      long timeOffset = 0;

      for (int chunk = 0; chunk < chunkOffsets.length; chunk++) {
        int chunkSampleCount = chunkingIterator.nextSampleCount();

        chunkSamples[chunk] = buildChunkSampleSizes(chunkSampleCount, sampleOffset, sampleSize, sampleSizes);
        chunkTimecodes[chunk] = timeOffset;

        timeOffset += calculateChunkDuration(chunkSampleCount, durationIterator);
        sampleOffset += chunkSampleCount;
      }

      return new TrackSeekInfo(timeOffset, chunkOffsets, chunkTimecodes, chunkSamples);
    }

    private static int[] buildChunkSampleSizes(int sampleCount, int sampleOffset, int sampleSize, int[] sampleSizes) {
      int[] chunkSampleSizes = new int[sampleCount];

      if (sampleSize != 0) {
        for (int i = 0; i < sampleCount; i++) {
          chunkSampleSizes[i] = sampleSize;
        }
      } else {
        System.arraycopy(sampleSizes, sampleOffset, chunkSampleSizes, 0, sampleCount);
      }

      return chunkSampleSizes;
    }

    private static int calculateChunkDuration(int sampleCount, SampleDurationIterator durationIterator) {
      int duration = 0;

      for (int i = 0; i < sampleCount; i++) {
        duration += durationIterator.nextSampleDuration();
      }

      return duration;
    }
  }

  private static class SampleChunkingIterator {
    private final int[] sampleChunkingFirst;
    private final int[] sampleChunkingCount;
    private int chunkIndex = 1;
    private int entryIndex = 0;

    private SampleChunkingIterator(int[] sampleChunkingFirst, int[] sampleChunkingCount) {
      this.sampleChunkingFirst = sampleChunkingFirst;
      this.sampleChunkingCount = sampleChunkingCount;
    }

    private int nextSampleCount() {
      int result = sampleChunkingCount[entryIndex];
      chunkIndex++;

      if (entryIndex + 1 < sampleChunkingFirst.length && chunkIndex == sampleChunkingFirst[entryIndex + 1]) {
        entryIndex++;
      }

      return result;
    }
  }

  private static class SampleDurationIterator {
    private final int[] sampleTimeCounts;
    private final int[] sampleTimeDeltas;
    private int relativeSampleIndex = 0;
    private int entryIndex = 0;

    private SampleDurationIterator(int[] sampleTimeCounts, int[] sampleTimeDeltas) {
      this.sampleTimeCounts = sampleTimeCounts;
      this.sampleTimeDeltas = sampleTimeDeltas;
    }

    private int nextSampleDuration() {
      int result = sampleTimeDeltas[entryIndex];

      if (entryIndex + 1 < sampleTimeCounts.length && ++relativeSampleIndex >= sampleTimeCounts[entryIndex]) {
        entryIndex++;
      }

      return result;
    }
  }
}
