package com.sedmelluq.discord.lavaplayer.container.flac.frame;

import com.sedmelluq.discord.lavaplayer.container.flac.frame.FlacFrameInfo.ChannelDelta;
import com.sedmelluq.discord.lavaplayer.container.flac.FlacStreamInfo;
import com.sedmelluq.discord.lavaplayer.tools.io.BitStreamReader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Handles reading one FLAC audio frame.
 */
public class FlacFrameReader {
  public static final int TEMPORARY_BUFFER_SIZE = 32;

  /**
   * Reads one frame, returning the number of samples written to sampleBuffers. A return value of 0 indicates that EOF
   * was reached in the frame, which happens when the track ends.
   *
   * @param inputStream Input stream for reading the track
   * @param reader Bit stream reader for the same underlying stream as inputStream
   * @param streamInfo Global stream information
   * @param rawSampleBuffers Intermediate sample decoding buffers. FlacStreamInfo#channelCount integer buffers of size
   *                         at least FlacStreamInfo#maximumBlockSize.
   * @param sampleBuffers The sample buffers where the final decoding result is written to. FlacStreamInfo#channelCount
   *                      short buffers of size at least FlacStreamInfo#maximumBlockSize.
   * @param temporaryBuffer Temporary working buffer of size at least TEMPORARY_BUFFER_SIZE. No state is held in this
   *                        between separate calls.
   * @return The number of samples read, zero on EOF
   * @throws IOException On read error
   */
  public static int readFlacFrame(InputStream inputStream, BitStreamReader reader, FlacStreamInfo streamInfo,
                                  int[][] rawSampleBuffers, short[][] sampleBuffers, int[] temporaryBuffer) throws IOException {
    FlacFrameInfo frameInfo = findAndParseFrameHeader(inputStream, reader, streamInfo);

    if (frameInfo == null) {
      return 0;
    }

    for (int i = 0; i < streamInfo.channelCount; i++) {
      FlacSubFrameReader.readSubFrame(reader, streamInfo, frameInfo, rawSampleBuffers[i], i, temporaryBuffer);
    }

    reader.readRemainingBits();
    reader.asInteger(16);

    applyChannelDelta(frameInfo.channelDelta, rawSampleBuffers, frameInfo.sampleCount);
    convertToShortPcm(streamInfo, frameInfo.sampleCount, rawSampleBuffers, sampleBuffers);

    return frameInfo.sampleCount;
  }

  private static FlacFrameInfo findAndParseFrameHeader(InputStream inputStream, BitStreamReader reader,
                                                       FlacStreamInfo streamInfo) throws IOException {
    int blockingStrategy;

    if ((blockingStrategy = skipToFrameSync(inputStream)) == -1) {
      return null;
    }

    return FlacFrameHeaderReader.readFrameHeader(reader, streamInfo, blockingStrategy == 1);
  }

  private static int skipToFrameSync(InputStream inputStream) throws IOException {
    int lastByte = -1;
    int currentByte;

    while ((currentByte = inputStream.read()) != -1) {
      if (lastByte == 0xFF && (currentByte & 0xFE) == 0xF8) {
        return currentByte & 0x01;
      }
      lastByte = currentByte;
    }

    return -1;
  }

  private static void applyChannelDelta(ChannelDelta channelDelta, int[][] rawSampleBuffers, int sampleCount) {
    switch (channelDelta) {
      case LEFT_SIDE:
        applyLeftSideDelta(rawSampleBuffers, sampleCount);
        break;
      case RIGHT_SIDE:
        applyRightSideDelta(rawSampleBuffers, sampleCount);
        break;
      case MID_SIDE:
        applyMidDelta(rawSampleBuffers, sampleCount);
        break;
      case NONE:
      default:
        break;
    }
  }

  private static void applyLeftSideDelta(int[][] rawSampleBuffers, int sampleCount) {
    for (int i = 0; i < sampleCount; i++) {
      rawSampleBuffers[1][i] = rawSampleBuffers[0][i] - rawSampleBuffers[1][i];
    }
  }

  private static void applyRightSideDelta(int[][] rawSampleBuffers, int sampleCount) {
    for (int i = 0; i < sampleCount; i++) {
      rawSampleBuffers[0][i] += rawSampleBuffers[1][i];
    }
  }

  private static void applyMidDelta(int[][] rawSampleBuffers, int sampleCount) {
    for (int i = 0; i < sampleCount; i++) {
      int delta = rawSampleBuffers[1][i];
      int middle = (rawSampleBuffers[0][i] << 1) + (delta & 1);

      rawSampleBuffers[0][i] = (middle + delta) >> 1;
      rawSampleBuffers[1][i] = (middle - delta) >> 1;
    }
  }

  private static void convertToShortPcm(FlacStreamInfo streamInfo, int sampleCount, int[][] rawSampleBuffers, short[][] sampleBuffers) {
    if (streamInfo.bitsPerSample < 16) {
      increaseSampleSize(streamInfo, sampleCount, rawSampleBuffers, sampleBuffers);
    } else if (streamInfo.bitsPerSample > 16) {
      decreaseSampleSize(streamInfo, sampleCount, rawSampleBuffers, sampleBuffers);
    } else {
      for (int channel = 0; channel < streamInfo.channelCount; channel++) {
        for (int i = 0; i < sampleCount; i++) {
          sampleBuffers[channel][i] = (short) rawSampleBuffers[channel][i];
        }
      }
    }
  }

  private static void increaseSampleSize(FlacStreamInfo streamInfo, int sampleCount, int[][] rawSampleBuffers, short[][] sampleBuffers) {
    int shiftLeft = 16 - streamInfo.bitsPerSample;

    for (int channel = 0; channel < streamInfo.channelCount; channel++) {
      for (int i = 0; i < sampleCount; i++) {
        sampleBuffers[channel][i] = (short) (rawSampleBuffers[channel][i] << shiftLeft);
      }
    }
  }

  private static void decreaseSampleSize(FlacStreamInfo streamInfo, int sampleCount, int[][] rawSampleBuffers, short[][] sampleBuffers) {
    int shiftRight = streamInfo.bitsPerSample - 16;

    for (int channel = 0; channel < streamInfo.channelCount; channel++) {
      for (int i = 0; i < sampleCount; i++) {
        sampleBuffers[channel][i] = (short) (rawSampleBuffers[channel][i] >> shiftRight);
      }
    }
  }
}
