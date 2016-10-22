package com.sedmelluq.discord.lavaplayer.container.flac;

import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection.checkNextBytes;
import static com.sedmelluq.discord.lavaplayer.container.flac.FlacMetadataHeader.BLOCK_COMMENT;
import static com.sedmelluq.discord.lavaplayer.container.flac.FlacMetadataHeader.BLOCK_SEEKTABLE;

/**
 * Loads either FLAC header information or a FLAC track object from a stream.
 */
public class FlacFileLoader {
  private static final Charset CHARSET = Charset.forName("UTF-8");

  static final int[] FLAC_CC = new int[] { 0x66, 0x4C, 0x61, 0x43 };

  private final SeekableInputStream inputStream;
  private final DataInput dataInput;
  private final byte[] headerBytes;
  private FlacMetadataHeader lastMetadataHeader;

  /**
   * @param inputStream Input stream to read the FLAC data from. This must be positioned right before FLAC FourCC.
   */
  public FlacFileLoader(SeekableInputStream inputStream) {
    this.inputStream = inputStream;
    this.dataInput = new DataInputStream(inputStream);
    this.headerBytes = new byte[FlacMetadataHeader.LENGTH];
  }

  /**
   * Read all metadata from a FLAC file. Stream position is at the beginning of the first frame after this call.
   * @return FLAC track information
   * @throws IOException On IO Error
   */
  public FlacTrackInfo parseHeaders() throws IOException {
    if (!checkNextBytes(inputStream, FLAC_CC, false)) {
      throw new IllegalStateException("Not a FLAC file");
    }

    TrackInfoBuilder trackInfoBuilder = new TrackInfoBuilder(readStreamInfoBlock());
    readMetadataBlocks(trackInfoBuilder);
    trackInfoBuilder.firstFramePosition = inputStream.getPosition();
    return trackInfoBuilder.build();
  }

  /**
   * Initialise a FLAC track stream.
   * @param context Configuration and output information for processing
   * @return The FLAC track stream which can produce frames.
   * @throws IOException On IO error
   */
  public FlacTrackStream loadTrack(AudioProcessingContext context) throws IOException {
    return new FlacTrackStream(context, parseHeaders(), inputStream);
  }

  private FlacStreamInfo readStreamInfoBlock() throws IOException {
    readMetadataHeader();

    if (lastMetadataHeader.blockType != 0) {
      throw new IllegalStateException("Wrong metadata block, should be stream info.");
    } else if (lastMetadataHeader.blockLength != FlacStreamInfo.LENGTH) {
      throw new IllegalStateException("Invalid stream info block size.");
    }

    byte[] streamInfoData = new byte[FlacStreamInfo.LENGTH];
    dataInput.readFully(streamInfoData);
    return new FlacStreamInfo(streamInfoData);
  }

  private void readMetadataHeader() throws IOException {
    dataInput.readFully(headerBytes);
    lastMetadataHeader = new FlacMetadataHeader(headerBytes);
  }

  private void readMetadataBlocks(TrackInfoBuilder trackInfoBuilder) throws IOException {
    while (!lastMetadataHeader.isLastBlock) {
      readMetadataHeader();
      long endPosition = inputStream.getPosition() + lastMetadataHeader.blockLength;

      if (lastMetadataHeader.blockType == BLOCK_SEEKTABLE) {
        readSeekTableBlock(trackInfoBuilder, lastMetadataHeader.blockLength);
      } else if (lastMetadataHeader.blockType == BLOCK_COMMENT) {
        readCommentBlock(trackInfoBuilder);
      }

      inputStream.seek(endPosition);
    }
  }

  private void readCommentBlock(TrackInfoBuilder trackInfoBuilder) throws IOException {
    int vendorLength = Integer.reverseBytes(dataInput.readInt());
    inputStream.skipFully(vendorLength);

    int listLength = Integer.reverseBytes(dataInput.readInt());

    for (int i = 0; i < listLength; i++) {
      int itemLength = Integer.reverseBytes(dataInput.readInt());

      byte[] textBytes = new byte[itemLength];
      dataInput.readFully(textBytes);

      String text = new String(textBytes, 0, textBytes.length, CHARSET);
      String[] keyAndValue = text.split("=", 2);

      if (keyAndValue.length > 1) {
        trackInfoBuilder.tags.put(keyAndValue[0].toUpperCase(), keyAndValue[1]);
      }
    }
  }

  private void readSeekTableBlock(TrackInfoBuilder trackInfoBuilder, int length) throws IOException {
    trackInfoBuilder.seekPoints = new FlacSeekPoint[length / FlacSeekPoint.LENGTH];

    for (int i = 0; i < trackInfoBuilder.seekPoints.length; i++) {
      long sampleIndex = dataInput.readLong();
      long byteOffset = dataInput.readLong();
      int sampleCount = dataInput.readUnsignedShort();

      trackInfoBuilder.seekPoints[i] = new FlacSeekPoint(sampleIndex, byteOffset, sampleCount);

      if (sampleIndex != -1) {
        trackInfoBuilder.seekPointCount = i + 1;
      }
    }
  }

  private static class TrackInfoBuilder {
    private final FlacStreamInfo streamInfo;
    private final Map<String, String> tags;
    private FlacSeekPoint[] seekPoints;
    private int seekPointCount;
    private long firstFramePosition;

    private TrackInfoBuilder(FlacStreamInfo streamInfo) {
      this.streamInfo = streamInfo;
      this.tags = new HashMap<>();
    }

    private FlacTrackInfo build() {
      return new FlacTrackInfo(streamInfo, seekPoints, seekPointCount, tags, firstFramePosition);
    }
  }
}
