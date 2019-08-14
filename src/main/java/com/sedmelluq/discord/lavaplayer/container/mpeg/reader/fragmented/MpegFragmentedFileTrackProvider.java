package com.sedmelluq.discord.lavaplayer.container.mpeg.reader.fragmented;

import com.sedmelluq.discord.lavaplayer.container.mpeg.reader.MpegFileTrackProvider;
import com.sedmelluq.discord.lavaplayer.container.mpeg.reader.MpegReader;
import com.sedmelluq.discord.lavaplayer.container.mpeg.reader.MpegSectionInfo;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegTrackConsumer;
import com.sedmelluq.discord.lavaplayer.container.mpeg.reader.MpegVersionedSectionInfo;
import com.sedmelluq.discord.lavaplayer.tools.io.DetachedByteChannel;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Track provider for fragmented MP4 file format.
 */
public class MpegFragmentedFileTrackProvider implements MpegFileTrackProvider {
  private final MpegReader reader;
  private final MpegSectionInfo root;

  private MpegTrackConsumer consumer;
  private boolean isFragmented;
  private long totalDuration;
  private MpegGlobalSeekInfo globalSeekInfo;
  private boolean seeking;
  private long minimumTimecode;

  /**
   * @param reader MP4-specific reader
   * @param root Root section info (synthetic section wrapping the entire file)
   */
  public MpegFragmentedFileTrackProvider(MpegReader reader, MpegSectionInfo root) {
    this.reader = reader;
    this.root = root;
  }

  @Override
  public boolean initialise(MpegTrackConsumer consumer) {
    if (!isFragmented || globalSeekInfo == null) {
      return false;
    }

    this.consumer = consumer;
    return true;
  }

  @Override
  public void provideFrames() throws InterruptedException {
    MpegSectionInfo moof;

    try (ReadableByteChannel channel = new DetachedByteChannel(Channels.newChannel(reader.seek))) {
      while ((moof = reader.nextChild(root)) != null) {
        if (!"moof".equals(moof.type)) {
          reader.skip(moof);
          continue;
        }

        MpegTrackFragmentHeader fragment = parseTrackMovieFragment(moof, consumer.getTrack().trackId);
        MpegSectionInfo mdat = reader.nextChild(root);

        long timecode = fragment.baseTimecode;
        reader.seek.seek(moof.offset + fragment.dataOffset);

        for (int i = 0; i < fragment.sampleSizes.length; i++) {
          handleSeeking(consumer, timecode);

          consumer.consume(channel, fragment.sampleSizes[i]);
        }

        reader.skip(mdat);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void seekToTimecode(long timecode) {
    minimumTimecode = timecode * globalSeekInfo.timescale / 1000;
    seeking = true;

    int segmentIndex;

    for (segmentIndex = 0; segmentIndex < globalSeekInfo.entries.length - 1; segmentIndex++) {
      if (globalSeekInfo.timeOffsets[segmentIndex + 1] > minimumTimecode) {
        break;
      }
    }

    try {
      reader.seek.seek(globalSeekInfo.fileOffsets[segmentIndex]);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public long getDuration() {
    return totalDuration * 1000 / globalSeekInfo.timescale;
  }

  /**
   * Handle mvex section.
   * @param mvex Section header.
   * @throws IOException On read error
   */
  public void parseMovieExtended(MpegSectionInfo mvex) throws IOException {
    reader.in(mvex).handleVersioned("trex", trex -> {
      isFragmented = true;
    }).run();
  }

  /**
   * Handle segment index section.
   * @param sbix Section header.
   * @throws IOException On read error
   */
  public void parseSegmentIndex(MpegVersionedSectionInfo sbix) throws IOException {
    reader.data.readInt(); // referenceId
    int timescale = reader.data.readInt();

    if (sbix.version == 0) {
      reader.data.readInt(); // earliestPresentationTime
      reader.data.readInt(); // firstOffset
    } else {
      reader.data.readLong(); // earliestPresentationTime
      reader.data.readLong(); // firstOffset
    }

    reader.data.readShort(); // reserved

    MpegSegmentEntry[] entries = new MpegSegmentEntry[reader.data.readUnsignedShort()];

    for (int i = 0; i < entries.length; i++) {
      int typeAndSize = reader.data.readInt();
      int duration = reader.data.readInt();
      reader.data.readInt(); // startsWithSap + sapType + sapDeltaTime

      entries[i] = new MpegSegmentEntry(typeAndSize >>> 31, typeAndSize & 0x7fffffff, duration);

      totalDuration += duration;
    }

    globalSeekInfo = new MpegGlobalSeekInfo(timescale, sbix.offset + sbix.length, entries);
  }

  private void handleSeeking(MpegTrackConsumer consumer, long timecode) {
    if (seeking) {
      // Even though sample durations may be available, decoding doesn't work if we don't start from the beginning
      // of a fragment. Therefore skipping within the fragment is handled by skipping decoded samples later.
      consumer.seekPerformed(minimumTimecode * 1000 / globalSeekInfo.timescale, timecode * 1000 / globalSeekInfo.timescale);
      seeking = false;
    }
  }

  private MpegTrackFragmentHeader parseTrackMovieFragment(MpegSectionInfo moof, int trackId) throws IOException {
    final AtomicReference<MpegTrackFragmentHeader> header = new AtomicReference<>();

    reader.in(moof).handle("traf", traf -> {
      final MpegTrackFragmentHeader.Builder builder = new MpegTrackFragmentHeader.Builder();

      reader.in(traf).handleVersioned("tfhd", tfhd -> {
        parseTrackFragmentHeader(tfhd, builder);
      }).handleVersioned("tfdt", tfdt -> {
        builder.setBaseTimecode((tfdt.version == 1) ? reader.data.readLong() : reader.data.readInt());
      }).handleVersioned("trun", trun -> {
        if (builder.getTrackId() == trackId) {
          parseTrackRunInfo(trun, builder);
        }
      }).run();

      if (builder.getTrackId() == trackId) {
        header.set(builder.build());
      }
    }).run();

    return header.get();
  }

  private void parseTrackFragmentHeader(MpegVersionedSectionInfo tfhd, MpegTrackFragmentHeader.Builder builder) throws IOException {
    builder.setTrackId(reader.data.readInt());

    if ((tfhd.flags & 0x000010) != 0) {
      // Need to read default sample size, but first must skip the fields before it
      if ((tfhd.flags & 0x000001) != 0) {
        // Skip baseDataOffset
        reader.data.readLong();
      }

      if ((tfhd.flags & 0x000002) != 0) {
        // Skip sampleDescriptionIndex
        reader.data.readInt();
      }

      if ((tfhd.flags & 0x000008) != 0) {
        // Skip defaultSampleDuration
        reader.data.readInt();
      }

      builder.setDefaultSampleSize(reader.data.readInt());
    }
  }

  private void parseTrackRunInfo(MpegVersionedSectionInfo trun, MpegTrackFragmentHeader.Builder builder) throws IOException {
    int sampleCount = reader.data.readInt();
    builder.setDataOffset(((trun.flags & 0x01) != 0) ? reader.data.readInt() : -1);

    if ((trun.flags & 0x04) != 0) {
      reader.data.skipBytes(4); // first sample flags
    }

    boolean hasDurations = (trun.flags & 0x100) != 0;
    boolean hasSizes = (trun.flags & 0x200) != 0;

    builder.createSampleArrays(hasDurations, hasSizes, sampleCount);

    for (int i = 0; i < sampleCount; i++) {
      if (hasDurations) {
        builder.setDuration(i, reader.data.readInt());
      }
      if (hasSizes) {
        builder.setSize(i, reader.data.readInt());
      }
      if ((trun.flags & 0x400) != 0) {
        reader.data.skipBytes(4);
      }
      if ((trun.flags & 0x800) != 0) {
        reader.data.skipBytes(4);
      }
    }
  }
}
