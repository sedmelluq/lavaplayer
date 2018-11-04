package com.sedmelluq.discord.lavaplayer.container.matroska;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sedmelluq.discord.lavaplayer.container.matroska.format.MatroskaBlock;
import com.sedmelluq.discord.lavaplayer.container.matroska.format.MatroskaCuePoint;
import com.sedmelluq.discord.lavaplayer.container.matroska.format.MatroskaElement;
import com.sedmelluq.discord.lavaplayer.container.matroska.format.MatroskaElementType;
import com.sedmelluq.discord.lavaplayer.container.matroska.format.MatroskaFileReader;
import com.sedmelluq.discord.lavaplayer.container.matroska.format.MatroskaFileTrack;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;

/**
 * Handles processing an MKV/WEBM file for the purpose of streaming one specific track from it. Only performs seeks when
 * absolutely necessary, as the stream may be a network connection, in which case each seek may require a new connection.
 */
public class MatroskaStreamingFile {
  private final MatroskaFileReader reader;

  private long timecodeScale = 1000000;
  private double duration;
  private final ArrayList<MatroskaFileTrack> trackList = new ArrayList<>();
  private MatroskaElement segmentElement = null;
  private MatroskaElement firstClusterElement = null;

  private long minimumTimecode = 0;
  private boolean seeking = false;

  private Long cueElementPosition = null;
  private List<MatroskaCuePoint> cuePoints = null;

  /**
   * @param inputStream The input stream for the file
   */
  public MatroskaStreamingFile(SeekableInputStream inputStream) {
    this.reader = new MatroskaFileReader(inputStream);
  }

  /**
   * @return Timescale for the durations used in this file
   */
  public long getTimecodeScale() {
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
   *
   * @throws IOException On read error.
   */
  public void readFile() throws IOException {
    MatroskaElement ebmlElement = reader.readNextElement(null);
    if (ebmlElement == null) {
      throw new RuntimeException("Unable to scan for EBML elements");
    }

    if (ebmlElement.is(MatroskaElementType.Ebml)) {
      parseEbmlElement(ebmlElement);
    } else {
      throw new RuntimeException("EBML Header not the first element in the file");
    }

    segmentElement = reader.readNextElement(null).frozen();

    if (segmentElement.is(MatroskaElementType.Segment)) {
      parseSegmentElement(segmentElement);
    }
    else {
      throw new RuntimeException(String.format("Segment not the second element in the file: was %s (%d) instead",
          segmentElement.getType().name(), segmentElement.getId()));
    }
  }

  private void parseEbmlElement(MatroskaElement ebmlElement) throws IOException {
    MatroskaElement child;

    while ((child = reader.readNextElement(ebmlElement)) != null) {
      if (child.is(MatroskaElementType.DocType)) {
        String docType = reader.asString(child);

        if (docType.compareTo("matroska") != 0 && docType.compareTo("webm") != 0) {
          throw new RuntimeException("Error: DocType is not matroska, \"" + docType + "\"");
        }
      }

      reader.skip(child);
    }
  }

  private void parseSegmentElement(MatroskaElement segmentElement) throws IOException {
    MatroskaElement child;

    while ((child = reader.readNextElement(segmentElement)) != null) {
      if (child.is(MatroskaElementType.Info)) {
        parseSegmentInfo(child);
      } else if (child.is(MatroskaElementType.Tracks)) {
        parseTracks(child);
      } else if (child.is(MatroskaElementType.Cluster)) {
        firstClusterElement = child.frozen();
        break;
      } else if (child.is(MatroskaElementType.SeekHead)) {
        parseSeekInfoForCuePosition(child);
      } else if (child.is(MatroskaElementType.Cues)) {
        cuePoints = parseCues(child);
      }

      reader.skip(child);
    }
  }

  private void parseSeekInfoForCuePosition(MatroskaElement seekHeadElement) throws IOException {
    MatroskaElement child;

    while ((child = reader.readNextElement(seekHeadElement)) != null) {
      if (child.is(MatroskaElementType.Seek)) {
        parseSeekElement(child);
      }

      reader.skip(child);
    }
  }

  private void parseSeekElement(MatroskaElement seekElement) throws IOException {
    MatroskaElement child;
    boolean isCueElement = false;

    while ((child = reader.readNextElement(seekElement)) != null) {
      if (child.is(MatroskaElementType.SeekId)) {
        isCueElement = ByteBuffer.wrap(reader.asBytes(child)).equals(ByteBuffer.wrap(MatroskaElementType.Cues.bytes));
      } else if (child.is(MatroskaElementType.SeekPosition) && isCueElement) {
        cueElementPosition = reader.asLong(child);
      }

      reader.skip(child);
    }
  }

  private List<MatroskaCuePoint> parseCues(MatroskaElement cuesElement) throws IOException {
    List<MatroskaCuePoint> parsedCuePoints = new ArrayList<>();
    MatroskaElement child;

    while ((child = reader.readNextElement(cuesElement)) != null) {
      if (child.is(MatroskaElementType.CuePoint)) {
        MatroskaCuePoint cuePoint = parseCuePoint(child);

        if (cuePoint != null) {
          parsedCuePoints.add(cuePoint);
        }
      }

      reader.skip(child);
    }

    return parsedCuePoints.isEmpty() ? null : parsedCuePoints;
  }

  private MatroskaCuePoint parseCuePoint(MatroskaElement cuePointElement) throws IOException {
    MatroskaElement child;

    Long cueTime = null;
    long[] positions = null;

    while ((child = reader.readNextElement(cuePointElement)) != null) {
      if (child.is(MatroskaElementType.CueTime)) {
        cueTime = reader.asLong(child);
      } else if (child.is(MatroskaElementType.CueTrackPositions)) {
        positions = parseCueTrackPositions(child);
      }

      reader.skip(child);
    }

    if (cueTime != null && positions != null) {
      return new MatroskaCuePoint(cueTime, positions);
    } else {
      return null;
    }
  }

  private long[] parseCueTrackPositions(MatroskaElement positionsElement) throws IOException {
    Long currentTrackId = null;
    MatroskaElement child;

    long[] positions = new long[trackList.size() + 1];
    Arrays.fill(positions, -1);

    while ((child = reader.readNextElement(positionsElement)) != null) {
      if (child.is(MatroskaElementType.CueTrack)) {
        currentTrackId = reader.asLong(child);
      } else if (child.is(MatroskaElementType.CueClusterPosition) && currentTrackId != null) {
        positions[currentTrackId.intValue()] = reader.asLong(child);
      }

      reader.skip(child);
    }

    return positions;
  }

  /**
   * Perform a seek to a specified timecode
   * @param trackId ID of the reference track
   * @param timecode Timecode using the timescale of the file
   */
  public void seekToTimecode(int trackId, long timecode) {
    try {
      seekToTimecodeInternal(trackId, timecode);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void seekToTimecodeInternal(int trackId, long timecode) throws IOException {
    minimumTimecode = timecode;
    seeking = true;

    if (cuePoints == null && cueElementPosition != null) {
      reader.seek(segmentElement.getPosition() + cueElementPosition);

      MatroskaElement cuesElement = reader.readNextElement(segmentElement);
      if (!cuesElement.is(MatroskaElementType.Cues)) {
        throw new IllegalStateException("The element here should be cue.");
      }

      cuePoints = parseCues(cuesElement);
    }

    if (cuePoints != null) {
      MatroskaCuePoint cuePoint = lastCueNotAfterTimecode(timecode);

      if (cuePoint != null && cuePoint.trackClusterOffsets[trackId] >= 0) {
        reader.seek(segmentElement.getDataPosition() + cuePoint.trackClusterOffsets[trackId]);
        return;
      }
    }

    // If there were no cues available, just seek to the beginning and discard until the right timecode
    reader.seek(firstClusterElement.getPosition());
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
   * @throws InterruptedException When interrupted externally (or for seek/stop).
   */
  public void provideFrames(MatroskaTrackConsumer consumer) throws InterruptedException {
    try {
      long position = reader.getPosition();
      MatroskaElement child = position == firstClusterElement.getDataPosition()
          ? firstClusterElement : reader.readNextElement(segmentElement);

      while (child != null) {
        if (child.is(MatroskaElementType.Cluster)) {
          parseNextCluster(child, consumer);
        }

        reader.skip(child);

        if (segmentElement.getRemaining(reader.getPosition()) <= 0) {
          break;
        }

        child = reader.readNextElement(segmentElement);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void parseNextCluster(MatroskaElement clusterElement, MatroskaTrackConsumer consumer) throws InterruptedException, IOException {
    MatroskaElement child;
    long clusterTimecode = 0;

    while ((child = reader.readNextElement(clusterElement)) != null) {
      if (child.is(MatroskaElementType.Timecode)) {
        clusterTimecode = reader.asLong(child);
      } else if (child.is(MatroskaElementType.SimpleBlock)) {
        parseClusterSimpleBlock(child, consumer, clusterTimecode);
      } else if (child.is(MatroskaElementType.BlockGroup)) {
        parseClusterBlockGroup(child, consumer, clusterTimecode);
      }

      reader.skip(child);
    }
  }

  private void parseClusterSimpleBlock(MatroskaElement simpleBlock, MatroskaTrackConsumer consumer, long clusterTimecode)
      throws InterruptedException, IOException {

    MatroskaBlock block = reader.readBlockHeader(simpleBlock, consumer.getTrack().index);

    if (block != null) {
      processFrameInBlock(block, consumer, clusterTimecode);
    }
  }

  private void parseClusterBlockGroup(MatroskaElement blockGroup, MatroskaTrackConsumer consumer, long clusterTimecode)
      throws InterruptedException, IOException {

    MatroskaElement child;

    while ((child = reader.readNextElement(blockGroup)) != null) {
      if (child.is(MatroskaElementType.Block)) {
        MatroskaBlock block = reader.readBlockHeader(child, consumer.getTrack().index);

        if (block != null) {
          processFrameInBlock(block, consumer, clusterTimecode);
        }
      }

      reader.skip(child);
    }
  }

  private void processFrameInBlock(MatroskaBlock block, MatroskaTrackConsumer consumer, long clusterTimecode)
      throws InterruptedException, IOException {

    long timecode = clusterTimecode + block.getTimecode();

    if (timecode >= minimumTimecode) {
      int frameCount = block.getFrameCount();

      if (seeking) {
        consumer.seekPerformed(minimumTimecode, timecode);
        seeking = false;
      }

      for (int i = 0; i < frameCount; i++) {
        consumer.consume(block.getNextFrameBuffer(reader, i));
      }
    }
  }

  private void parseSegmentInfo(MatroskaElement infoElement) throws IOException {
    MatroskaElement child;

    while ((child = reader.readNextElement(infoElement)) != null) {
      if (child.is(MatroskaElementType.Duration)) {
        duration = reader.asDouble(child);
      } else if (child.is(MatroskaElementType.TimecodeScale)) {
        timecodeScale = reader.asLong(child);
      }

      reader.skip(child);
    }
  }

  private void parseTracks(MatroskaElement tracksElement) throws IOException {
    MatroskaElement child;

    while ((child = reader.readNextElement(tracksElement)) != null) {
      if (child.is(MatroskaElementType.TrackEntry)) {
        trackList.add(MatroskaFileTrack.parse(child, reader));
      }

      reader.skip(child);
    }
  }
}
