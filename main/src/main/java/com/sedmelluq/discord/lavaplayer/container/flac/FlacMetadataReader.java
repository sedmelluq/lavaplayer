package com.sedmelluq.discord.lavaplayer.container.flac;

import org.apache.commons.io.IOUtils;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static com.sedmelluq.discord.lavaplayer.container.flac.FlacMetadataHeader.BLOCK_COMMENT;
import static com.sedmelluq.discord.lavaplayer.container.flac.FlacMetadataHeader.BLOCK_SEEKTABLE;

/**
 * Handles reading one FLAC metadata blocks.
 */
public class FlacMetadataReader {
  private static final Charset CHARSET = StandardCharsets.UTF_8;

  /**
   * Reads FLAC stream info metadata block.
   *
   * @param dataInput Data input where the block is read from
   * @return Stream information
   * @throws IOException On read error
   */
  public static FlacStreamInfo readStreamInfoBlock(DataInput dataInput) throws IOException {
    FlacMetadataHeader header = readMetadataHeader(dataInput);

    if (header.blockType != 0) {
      throw new IllegalStateException("Wrong metadata block, should be stream info.");
    } else if (header.blockLength != FlacStreamInfo.LENGTH) {
      throw new IllegalStateException("Invalid stream info block size.");
    }

    byte[] streamInfoData = new byte[FlacStreamInfo.LENGTH];
    dataInput.readFully(streamInfoData);
    return new FlacStreamInfo(streamInfoData, !header.isLastBlock);
  }

  private static FlacMetadataHeader readMetadataHeader(DataInput dataInput) throws IOException {
    byte[] headerBytes = new byte[FlacMetadataHeader.LENGTH];
    dataInput.readFully(headerBytes);
    return new FlacMetadataHeader(headerBytes);
  }

  /**
   * @param dataInput Data input where the block is read from
   * @param inputStream Input stream matching the data input
   * @param trackInfoBuilder Track info builder object where detected metadata is stored in
   * @return True if there are more metadata blocks available
   * @throws IOException On read error
   */
  public static boolean readMetadataBlock(DataInput dataInput, InputStream inputStream, FlacTrackInfoBuilder trackInfoBuilder) throws IOException {
    FlacMetadataHeader header = readMetadataHeader(dataInput);

    if (header.blockType == BLOCK_SEEKTABLE) {
      readSeekTableBlock(dataInput, trackInfoBuilder, header.blockLength);
    } else if (header.blockType == BLOCK_COMMENT) {
      readCommentBlock(dataInput, inputStream, trackInfoBuilder);
    } else {
      IOUtils.skipFully(inputStream, header.blockLength);
    }

    return !header.isLastBlock;
  }

  private static void readCommentBlock(DataInput dataInput, InputStream inputStream, FlacTrackInfoBuilder trackInfoBuilder) throws IOException {
    int vendorLength = Integer.reverseBytes(dataInput.readInt());
    IOUtils.skipFully(inputStream, vendorLength);

    int listLength = Integer.reverseBytes(dataInput.readInt());

    for (int i = 0; i < listLength; i++) {
      int itemLength = Integer.reverseBytes(dataInput.readInt());

      byte[] textBytes = new byte[itemLength];
      dataInput.readFully(textBytes);

      String text = new String(textBytes, 0, textBytes.length, CHARSET);
      String[] keyAndValue = text.split("=", 2);

      if (keyAndValue.length > 1) {
        trackInfoBuilder.addTag(keyAndValue[0].toUpperCase(), keyAndValue[1]);
      }
    }
  }

  private static void readSeekTableBlock(DataInput dataInput, FlacTrackInfoBuilder trackInfoBuilder, int length) throws IOException {
    FlacSeekPoint[] seekPoints = new FlacSeekPoint[length / FlacSeekPoint.LENGTH];
    int seekPointCount = 0;

    for (int i = 0; i < seekPoints.length; i++) {
      long sampleIndex = dataInput.readLong();
      long byteOffset = dataInput.readLong();
      int sampleCount = dataInput.readUnsignedShort();

      seekPoints[i] = new FlacSeekPoint(sampleIndex, byteOffset, sampleCount);

      if (sampleIndex != -1) {
        seekPointCount = i + 1;
      }
    }

    trackInfoBuilder.setSeekPoints(seekPoints, seekPointCount);
  }
}
