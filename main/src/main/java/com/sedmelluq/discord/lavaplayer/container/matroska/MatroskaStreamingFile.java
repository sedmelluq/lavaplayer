package com.sedmelluq.discord.lavaplayer.container.matroska;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ebml.EBMLReader;
import org.ebml.Element;
import org.ebml.FloatElement;
import org.ebml.MasterElement;
import org.ebml.SignedIntegerElement;
import org.ebml.StringElement;
import org.ebml.UnsignedIntegerElement;
import org.ebml.io.DataSource;
import org.ebml.matroska.MatroskaDocTypes;
import org.ebml.matroska.MatroskaFileFrame;
import org.ebml.matroska.MatroskaFileTrack;

/**
 * Handles processing an MKV/WEBM file for the purpose of streaming one specific track from it. Only performs seeks when
 * absolutely necessary, as the stream may be a network connection, in which case each seek may require a new connection.
 */
public class MatroskaStreamingFile {
  static {
    MatroskaDocTypes.Void.getLevel();
  }

  private final DataSource dataSource;
  private final EBMLReader reader;

  private long timecodeScale = 1000000;
  private double duration;
  private final ArrayList<MatroskaFileTrack> trackList = new ArrayList<>();
  private Element firstClusterElement = null;

  private long minimumTimecode = 0;
  private boolean seeking = false;

  private Long segmentElementPosition = null;
  private Long segmentEndPosition = null;
  private Long firstClusterPosition = null;
  private Long cueElementPosition = null;
  private List<MatroskaCuePoint> cuePoints = null;

  /**
   * @param inputDataSource The data source for the file
   */
  public MatroskaStreamingFile(final DataSource inputDataSource) {
    dataSource = inputDataSource;
    reader = new EBMLReader(dataSource);
  }

  /**
   * @return Timescale for the durations used in this file
   */
  public long getTimecodeScale()
  {
    return timecodeScale;
  }

  /**
   * @return Total duration of the file
   */
  public double getDuration() {
    return duration;
  }

  /**
   * @return Array of tracks in this file
   */
  public MatroskaFileTrack[] getTrackList() {
    if (!trackList.isEmpty()) {
      MatroskaFileTrack[] tracks = new MatroskaFileTrack[trackList.size()];

      for (int t = 0; t < trackList.size(); t++) {
        tracks[t] = trackList.get(t);
      }
      return tracks;
    } else {
      return new MatroskaFileTrack[0];
    }
  }

  /**
   * Read the headers and tracks from the file.
   */
  public void readFile() {
    Element level0 = reader.readNextElement();
    if (level0 == null) {
      throw new RuntimeException("Unable to scan for EBML elements");
    }

    if (level0.isType(MatroskaDocTypes.EBML.getType())) {
      parseEbmlElement((MasterElement) level0);
    } else {
      throw new RuntimeException("EBML Header not the first element in the file");
    }

    level0 = reader.readNextElement();
    segmentElementPosition = dataSource.getFilePointer();
    segmentEndPosition = dataSource.getFilePointer() + level0.getSize();

    if (level0.isType(MatroskaDocTypes.Segment.getType())) {
      parseSegmentElement((MasterElement) level0);
    }
    else {
      throw new RuntimeException(String.format("Segment not the second element in the file: was %s instead",
          level0.getElementType().getName()));
    }
  }

  private void parseEbmlElement(MasterElement ebml) {
    Element level1 = ebml.readNextChild(reader);

    while (level1 != null) {
      level1.readData(dataSource);
      if (level1.isType(MatroskaDocTypes.DocType.getType())) {
        final String docType = ((StringElement) level1).getValue();

        if (docType.compareTo("matroska") != 0 && docType.compareTo("webm") != 0) {
          throw new RuntimeException("Error: DocType is not matroska, \"" + ((StringElement) level1).getValue() + "\"");
        }
      }

      level1 = ebml.readNextChild(reader);
    }
  }

  private void parseSegmentElement(MasterElement segment) {
    long currentPosition = dataSource.getFilePointer();
    Element level1 = segment.readNextChild(reader);

    while (level1 != null) {
      if (level1.isType(MatroskaDocTypes.Info.getType())) {
        parseSegmentInfo(level1);
      } else if (level1.isType(MatroskaDocTypes.Tracks.getType())) {
        parseTracks(level1);
      } else if (level1.isType(MatroskaDocTypes.Cluster.getType())) {
        firstClusterPosition = currentPosition;
        firstClusterElement = level1;
        break;
      } else if (level1.isType(MatroskaDocTypes.SeekHead.getType())) {
        parseSeekInfoForCuePosition((MasterElement) level1);
      } else if (level1.isType(MatroskaDocTypes.Cues.getType())) {
        cuePoints = parseCues((MasterElement) level1);
      }

      level1.skipData(dataSource);
      currentPosition = dataSource.getFilePointer();
      level1 = segment.readNextChild(reader);
    }
  }

  private void parseSeekInfoForCuePosition(MasterElement level1) {
    Element level2;

    while ((level2 = level1.readNextChild(reader)) != null) {
      if (level2.isType(MatroskaDocTypes.Seek.getType())) {
        parseSeekElement((MasterElement) level2);
      }

      level2.skipData(dataSource);
    }
  }

  private void parseSeekElement(MasterElement level2) {
    Element level3;
    boolean isCueElement = false;

    while ((level3 = level2.readNextChild(reader)) != null) {
      if (level3.isType(MatroskaDocTypes.SeekID.getType())) {
        level3.readData(dataSource);
        isCueElement = level3.getData().equals(MatroskaDocTypes.Cues.getType());
      } else if (level3.isType(MatroskaDocTypes.SeekPosition.getType()) && isCueElement) {
        level3.readData(dataSource);
        cueElementPosition = ((UnsignedIntegerElement) level3).getValue();
      }

      level3.skipData(dataSource);
    }
  }

  private List<MatroskaCuePoint> parseCues(MasterElement level1) {
    Element level2;
    List<MatroskaCuePoint> parsedCuePoints = new ArrayList<>();

    while ((level2 = level1.readNextChild(reader)) != null) {
      if (level2.isType(MatroskaDocTypes.CuePoint.getType())) {
        MatroskaCuePoint cuePoint = parseCuePoint((MasterElement) level2);

        if (cuePoint != null) {
          parsedCuePoints.add(cuePoint);
        }
      }

      level2.skipData(dataSource);
    }

    return parsedCuePoints.isEmpty() ? null : parsedCuePoints;
  }

  private MatroskaCuePoint parseCuePoint(MasterElement level2) {
    Element level3;

    Long cueTime = null;
    long[] positions = null;

    while ((level3 = level2.readNextChild(reader)) != null) {
      if (level3.isType(MatroskaDocTypes.CueTime.getType())) {
        level3.readData(dataSource);
        cueTime = ((UnsignedIntegerElement) level3).getValue();
      } else if (level3.isType(MatroskaDocTypes.CueTrackPositions.getType())) {
        positions = parseCueTrackPositions((MasterElement) level3);
      }

      level3.skipData(dataSource);
    }

    if (cueTime != null && positions != null) {
      return new MatroskaCuePoint(cueTime, positions);
    } else {
      return null;
    }
  }

  private long[] parseCueTrackPositions(MasterElement level3) {
    Long currentTrackId = null;
    Element level4;

    long[] positions = new long[trackList.size() + 1];
    Arrays.fill(positions, -1);

    while ((level4 = level3.readNextChild(reader)) != null) {
      if (level4.isType(MatroskaDocTypes.CueTrack.getType())) {
        level4.readData(dataSource);
        currentTrackId = ((UnsignedIntegerElement)level4).getValue();
      } else if (level4.isType(MatroskaDocTypes.CueClusterPosition.getType()) && currentTrackId != null) {
        level4.readData(dataSource);
        positions[currentTrackId.intValue()] = ((UnsignedIntegerElement)level4).getValue();
      }

      level4.skipData(dataSource);
    }

    return positions;
  }

  /**
   * Perform a seek to a specified timecode
   * @param trackId ID of the reference track
   * @param timecode Timecode using the timescale of the file
   */
  public void seekToTimecode(int trackId, long timecode) {
    minimumTimecode = timecode;
    seeking = true;

    firstClusterElement = null;

    if (cuePoints == null && cueElementPosition != null) {
      dataSource.seek(segmentElementPosition + cueElementPosition);

      Element level1 = reader.readNextElement();
      if (!level1.isType(MatroskaDocTypes.Cues.getType())) {
        throw new IllegalStateException("The element here should be cue.");
      }

      cuePoints = parseCues((MasterElement) level1);
    }

    if (cuePoints != null) {
      MatroskaCuePoint cuePoint = lastCueNotAfterTimecode(timecode);

      if (cuePoint != null && cuePoint.trackClusterOffsets[trackId] >= 0) {
        dataSource.seek(segmentElementPosition + cuePoint.trackClusterOffsets[trackId]);
        return;
      }
    }

    // If there were no cues available, just seek to the beginning and discard until the right timecode
    dataSource.seek(firstClusterPosition);
  }

  private MatroskaCuePoint lastCueNotAfterTimecode(long timecode) {
    int largerTimecodeIndex;

    for (largerTimecodeIndex = 0; largerTimecodeIndex < cuePoints.size(); largerTimecodeIndex++) {
      if (cuePoints.get(largerTimecodeIndex).timecode > timecode) {
        break;
      }
    }

    if (largerTimecodeIndex > 0) {
      return cuePoints.get(largerTimecodeIndex - 1);
    } else {
      return null;
    }
  }

  /**
   * Provide data chunks for the specified track consumer
   * @param consumer Track data consumer
   * @throws InterruptedException
   */
  public void provideFrames(MatroskaTrackConsumer consumer) throws InterruptedException {
    Element level1 = firstClusterElement != null ? firstClusterElement : reader.readNextElement();
    firstClusterElement = null;

    while (level1 != null) {
      if (level1.isType(MatroskaDocTypes.Cluster.getType())) {
        parseNextCluster((MasterElement) level1, consumer);
      }

      level1.skipData(dataSource);

      if (dataSource.getFilePointer() >= segmentEndPosition) {
        break;
      }

      level1 = reader.readNextElement();
    }
  }

  private void parseNextCluster(MasterElement cluster, MatroskaTrackConsumer consumer) throws InterruptedException {
    Element level2;

    long clusterTimecode = 0;
    level2 = cluster.readNextChild(reader);

    while (level2 != null) {
      if (level2.isType(MatroskaDocTypes.Timecode.getType())) {
        level2.readData(dataSource);
        clusterTimecode = ((UnsignedIntegerElement) level2).getValue();
      } else if (level2.isType(MatroskaDocTypes.SimpleBlock.getType())) {
        parseClusterSimpleBlock(level2, consumer, clusterTimecode);
      } else if (level2.isType(MatroskaDocTypes.BlockGroup.getType())) {
        parseClusterBlockGroup((MasterElement) level2, consumer, clusterTimecode);
      }

      level2.skipData(dataSource);
      level2 = cluster.readNextChild(reader);
    }
  }

  private void parseClusterSimpleBlock(Element simpleBlock, MatroskaTrackConsumer consumer, long clusterTimecode)
      throws InterruptedException {

    simpleBlock.readData(dataSource);
    final long blockDuration = 0;

    MatroskaFixedBlock block = new MatroskaFixedBlock(simpleBlock.getData());
    block.parseHeader();

    processFrameInBlock(block, consumer, clusterTimecode, blockDuration, false, 0);
  }

  private void parseClusterBlockGroup(MasterElement blockGroup, MatroskaTrackConsumer consumer, long clusterTimecode)
      throws InterruptedException {

    long blockDuration = 0;
    long blockReference = 0;
    Element level3 = blockGroup.readNextChild(reader);
    MatroskaFixedBlock block = null;

    while (level3 != null) {
      if (level3.isType(MatroskaDocTypes.Block.getType())) {
        level3.readData(dataSource);
        block = new MatroskaFixedBlock(level3.getData());
        block.parseHeader();
      } else if (level3.isType(MatroskaDocTypes.BlockDuration.getType())) {
        level3.readData(dataSource);
        blockDuration = ((UnsignedIntegerElement) level3).getValue();
      } else if (level3.isType(MatroskaDocTypes.ReferenceBlock.getType())) {
        level3.readData(dataSource);
        blockReference = ((SignedIntegerElement) level3).getValue();
      }

      level3.skipData(dataSource);
      level3 = blockGroup.readNextChild(reader);
    }

    if (block == null) {
      throw new NullPointerException("BlockGroup element with no child Block!");
    }

    processFrameInBlock(block, consumer, clusterTimecode, blockDuration, true, blockReference);
  }

  private void processFrameInBlock(MatroskaFixedBlock block, MatroskaTrackConsumer consumer, long clusterTimecode,
                                   long blockDuration, boolean hasReference, long blockReference) throws InterruptedException {
    if (consumer.getTrack().getTrackNo() != block.getTrackNumber()) {
      return;
    }

    long timecode = clusterTimecode + block.getTimecode();

    if (timecode >= minimumTimecode) {
      int frameCount = block.getFrameCount();

      MatroskaFileFrame frame = new MatroskaFileFrame();
      frame.setTrackNo(block.getTrackNumber());
      frame.setTimecode(timecode);
      frame.setDuration(blockDuration);
      frame.setKeyFrame(block.isKeyFrame());

      if (hasReference) {
        frame.addReferences(blockReference);
      }

      if (seeking) {
        consumer.seekPerformed(minimumTimecode, timecode);
        seeking = false;
      }

      for (int i = 0; i < frameCount; i++) {
        frame.setData(block.getFrameBuffer(i));
        consumer.consume(frame);
      }
    }
  }

  private void parseSegmentInfo(Element level1) {
    Element level2 = ((MasterElement) level1).readNextChild(reader);

    while (level2 != null) {
      if (level2.isType(MatroskaDocTypes.Duration.getType())) {
        level2.readData(dataSource);
        duration = ((FloatElement) level2).getValue();
      } else if (level2.isType(MatroskaDocTypes.TimecodeScale.getType())) {
        level2.readData(dataSource);
        timecodeScale = ((UnsignedIntegerElement) level2).getValue();
      }

      level2.skipData(dataSource);
      level2 = ((MasterElement) level1).readNextChild(reader);
    }
  }

  private void parseTracks(final Element level1) {
    Element level2 = ((MasterElement) level1).readNextChild(reader);

    try {
      Method fromElementMethod = MatroskaFileTrack.class.getDeclaredMethod("fromElement", Element.class, DataSource.class, EBMLReader.class);
      fromElementMethod.setAccessible(true);

      while (level2 != null) {
        if (level2.isType(MatroskaDocTypes.TrackEntry.getType())) {
          trackList.add((MatroskaFileTrack) fromElementMethod.invoke(null, level2, dataSource, reader));
        }

        level2.skipData(dataSource);
        level2 = ((MasterElement) level1).readNextChild(reader);
      }
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
}
