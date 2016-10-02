package com.sedmelluq.discord.lavaplayer.container.mpeg;

import com.sedmelluq.discord.lavaplayer.tools.io.DetachedByteChannel;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles processing an MP4 file for the purpose of streaming one specific track from it. Only performs seeks when
 * absolutely necessary, as the stream may be a network connection, in which case each seek may require a new connection.
 */
public class MpegStreamingFile {
  private final List<MpegTrackInfo> tracks;
  private final MpegReader reader;
  private final MpegSectionInfo root;

  private MpegGlobalSeekInfo globalSeekInfo;
  private boolean seeking;
  private long minimumTimecode;
  private boolean isFragmented;
  private long totalDuration;

  /**
   * @param inputStream Stream to read the file from
   */
  public MpegStreamingFile(SeekableInputStream inputStream) {
    this.tracks = new ArrayList<>();
    this.reader = new MpegReader(inputStream);
    this.root = new MpegSectionInfo(0, inputStream.getContentLength(), "root");
  }

  /**
   * @return List of tracks found in the file
   */
  public List<MpegTrackInfo> getTrackList() {
    return tracks;
  }

  /**
   * @return Whether this MP4 uses a fragmented format
   */
  public boolean isFragmented() {
    return isFragmented;
  }

  /**
   * @return Total duration in milliseconds
   */
  public long getDuration() {
    return totalDuration * 1000 / globalSeekInfo.timescale;
  }

  /**
   * Provide audio frames to the frame consumer until the end of the track or interruption.
   * @param consumer The track consumer to use
   */
  public void provideFrames(MpegTrackConsumer consumer) throws InterruptedException {
    MpegSectionInfo moof;

    try (ReadableByteChannel channel = new DetachedByteChannel(Channels.newChannel(reader.seek))) {
      while ((moof = reader.nextChild(root)) != null) {
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

  private void handleSeeking(MpegTrackConsumer consumer, long timecode) {
    if (seeking) {
      // Even though sample durations may be available, decoding doesn't work if we don't start from the beginning
      // of a fragment. Therefore skipping within the fragment is handled by skipping decoded samples later.
      consumer.seekPerformed(minimumTimecode * 1000 / globalSeekInfo.timescale, timecode * 1000 / globalSeekInfo.timescale);
      seeking = false;
    }
  }

  /**
   * Perform a seek to the given timecode (ms). On the next call to provideFrames, the seekPerformed method of frame
   * consumer is called with the position where it actually seeked to and the position where the seek was requested to
   * as arguments.
   * @param timecode The timecode to seek to in milliseconds
   */
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

  private MpegTrackFragmentHeader parseTrackMovieFragment(MpegSectionInfo moof, int trackId) throws IOException {
    final AtomicReference<MpegTrackFragmentHeader> header = new AtomicReference<>();

    reader.in(moof).handle("traf", traf -> {
      final MpegTrackFragmentHeader.Builder builder = new MpegTrackFragmentHeader.Builder();

      reader.in(traf).handleVersioned("tfhd", tfhd -> {
        builder.setTrackId(reader.data.readInt());
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

  /**
   * Read the headers of the file to get the list of tracks and data required for seeking.
   */
  public void readFile() {
    try {
      reader.in(root).handle("moov", moov -> {
        reader.in(moov).handle("trak",
            this::parseTrackInfo
        ).handle("mvex", mvex -> {
          reader.in(mvex).handleVersioned("trex", trex -> {
            isFragmented = true;
          }).run();
        }).run();
      }).handleVersioned("sidx", true,
          this::parseSegmentIndex
      ).run();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void parseSegmentIndex(MpegVersionedSectionInfo sbix) throws IOException {
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

  private void parseTrackInfo(MpegSectionInfo trak) throws IOException {
    final MpegTrackInfo.Builder trackInfo = new MpegTrackInfo.Builder();

    reader.in(trak).handleVersioned("tkhd", tkhd -> {
      reader.data.skipBytes(tkhd.version == 1 ? 16 : 8);

      trackInfo.setTrackId(reader.data.readInt());
    }).handle("mdia", mdia -> {
      reader.in(mdia).handleVersioned("hdlr", hdlr -> {
        reader.data.skipBytes(4);

        trackInfo.setHandler(reader.readFourCC());
      }).handle("minf", minf -> {
        reader.in(minf).handle("stbl", stbl -> {
          MpegReader.Chain chain = reader.in(stbl);
          parseTrackCodecInfo(chain, trackInfo);
          chain.run();
        }).run();
      }).run();
    }).run();

    tracks.add(trackInfo.build());
  }

  private void parseTrackCodecInfo(MpegReader.Chain chain, MpegTrackInfo.Builder trackInfo) {
    chain.handleVersioned("stsd", stsd -> {
      int entryCount = reader.data.readInt();
      if (entryCount > 0) {
        MpegSectionInfo codec = reader.nextChild(stsd);
        trackInfo.setCodecName(codec.type);

        if ("soun".equals(trackInfo.getHandler())) {
          parseSoundTrackCodec(codec, trackInfo);
        }
      }
    });
  }

  private void parseSoundTrackCodec(MpegSectionInfo codec, MpegTrackInfo.Builder trackInfo) throws IOException {
    reader.parseFlags(codec);

    reader.data.readUnsignedShort(); // data_reference_index
    reader.data.readUnsignedShort(); // apple: sound_version
    reader.data.skipBytes(8); // reserved

    trackInfo.setChannelCount(reader.data.readUnsignedShort());

    reader.data.readUnsignedShort(); // sample_size
    reader.data.readUnsignedShort(); // apple stuff

    trackInfo.setSampleRate(reader.data.readInt());
  }
}
