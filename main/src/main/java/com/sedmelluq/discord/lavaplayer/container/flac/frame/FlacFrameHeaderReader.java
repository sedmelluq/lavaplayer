package com.sedmelluq.discord.lavaplayer.container.flac.frame;

import com.sedmelluq.discord.lavaplayer.container.flac.frame.FlacFrameInfo.ChannelDelta;
import com.sedmelluq.discord.lavaplayer.container.flac.FlacStreamInfo;
import com.sedmelluq.discord.lavaplayer.tools.io.BitStreamReader;

import java.io.IOException;

import static com.sedmelluq.discord.lavaplayer.container.flac.frame.FlacFrameInfo.ChannelDelta.LEFT_SIDE;
import static com.sedmelluq.discord.lavaplayer.container.flac.frame.FlacFrameInfo.ChannelDelta.MID_SIDE;
import static com.sedmelluq.discord.lavaplayer.container.flac.frame.FlacFrameInfo.ChannelDelta.NONE;
import static com.sedmelluq.discord.lavaplayer.container.flac.frame.FlacFrameInfo.ChannelDelta.RIGHT_SIDE;

/**
 * Contains methods for reading a frame header.
 */
public class FlacFrameHeaderReader {
  private static final int VALUE_INVALID = Integer.MIN_VALUE;
  private static final int VALUE_INHERITED = -1024;

  private static final int BLOCK_SIZE_EXPLICIT_8_BIT = -2;
  private static final int BLOCK_SIZE_EXPLICIT_16_BIT = -1;

  private static final int SAMPLE_RATE_EXPLICIT_8_BIT = -3;
  private static final int SAMPLE_RATE_EXPLICIT_16_BIT = -2;
  private static final int SAMPLE_RATE_EXPLICIT_10X_16_BIT = -1;

  private static final int[] blockSizeMapping = new int[] {
      VALUE_INVALID, 192, 576, 1152, 2304, 4608, BLOCK_SIZE_EXPLICIT_8_BIT, BLOCK_SIZE_EXPLICIT_16_BIT,
      256, 512, 1024, 2048, 4096, 8192, 16384, 32768
  };

  private static final int[] sampleRateMapping = new int[] {
      VALUE_INHERITED, 88200, 176400, 192000, 8000, 16000, 22050, 24000,
      32000, 44100, 48000, 96000, SAMPLE_RATE_EXPLICIT_8_BIT, SAMPLE_RATE_EXPLICIT_16_BIT, SAMPLE_RATE_EXPLICIT_10X_16_BIT, VALUE_INVALID
  };

  private static final int[] channelCountMapping = new int[] {
      1, 2, 3, 4, 5, 6, 7, 8,
      2, 2, 2, VALUE_INVALID, VALUE_INVALID, VALUE_INVALID, VALUE_INVALID, VALUE_INVALID
  };

  private static final ChannelDelta[] channelDeltaMapping = new ChannelDelta[] {
      NONE, NONE, NONE, NONE, NONE, NONE, NONE, NONE,
      LEFT_SIDE, RIGHT_SIDE, MID_SIDE, NONE, NONE, NONE, NONE, NONE
  };

  private static final int[] sampleSizeMapping = new int[] { VALUE_INHERITED, 8, 12, VALUE_INVALID, 16, 20, 24, VALUE_INVALID };

  /**
   * Reads a frame header. At this point the first two bytes of the frame have actually been read during the frame sync
   * scanning already. This means that this method expects there to be no EOF in the middle of the header. The frame
   * information must match that of the stream, as changing sample rates, channel counts and sample sizes are not
   * supported.
   *
   * @param bitStreamReader Bit stream reader for input
   * @param streamInfo Information about the stream from metadata headers
   * @param variableBlock If this is a variable block header. This information was included in the frame sync bytes
   *                      consumed before calling this method.
   * @return The frame information.
   * @throws IOException On read error.
   */
  public static FlacFrameInfo readFrameHeader(BitStreamReader bitStreamReader, FlacStreamInfo streamInfo,
                                              boolean variableBlock) throws IOException {

    int blockSize = blockSizeMapping[bitStreamReader.asInteger(4)];
    int sampleRate = sampleRateMapping[bitStreamReader.asInteger(4)];
    int channelAssignment = bitStreamReader.asInteger(4);
    int channelCount = channelCountMapping[channelAssignment];
    ChannelDelta channelDelta = channelDeltaMapping[channelAssignment];
    int sampleSize = sampleSizeMapping[bitStreamReader.asInteger(3)];

    bitStreamReader.asInteger(1);

    readUtf8Value(variableBlock, bitStreamReader);

    if (blockSize == BLOCK_SIZE_EXPLICIT_8_BIT) {
      blockSize = bitStreamReader.asInteger(8);
    } else if (blockSize == BLOCK_SIZE_EXPLICIT_16_BIT) {
      blockSize = bitStreamReader.asInteger(16);
    }

    verifyNotInvalid(blockSize, "block size");

    if (blockSize == SAMPLE_RATE_EXPLICIT_8_BIT) {
      sampleRate = bitStreamReader.asInteger(8);
    } else if (blockSize == SAMPLE_RATE_EXPLICIT_16_BIT) {
      sampleRate = bitStreamReader.asInteger(16);
    } else if (blockSize == SAMPLE_RATE_EXPLICIT_10X_16_BIT) {
      sampleRate = bitStreamReader.asInteger(16) * 10;
    }

    verifyMatchesExpected(sampleRate, streamInfo.sampleRate, "sample rate");
    verifyMatchesExpected(channelCount, streamInfo.channelCount, "channel count");
    verifyMatchesExpected(sampleSize, streamInfo.bitsPerSample, "bits per sample");

    // Ignore CRC for now
    bitStreamReader.asInteger(8);

    return new FlacFrameInfo(blockSize, channelDelta);
  }

  private static void verifyNotInvalid(int value, String description) {
    if (value < 0) {
      throw new IllegalStateException("Invalid value " + value + " for " + description);
    }
  }

  private static void verifyMatchesExpected(int value, int expected, String description) {
    if (value != VALUE_INHERITED && value != expected) {
      throw new IllegalStateException("Invalid value " + value + " for " + description + ", should match value " + expected + " in stream.");
    }
  }

  private static long readUtf8Value(boolean isLong, BitStreamReader bitStreamReader) throws IOException {
    int maximumSize = isLong ? 7 : 6;
    int firstByte = bitStreamReader.asInteger(8);
    int leadingOnes = Integer.numberOfLeadingZeros((~firstByte) & 0xFF) - 24;

    if (leadingOnes > maximumSize || leadingOnes == 1) {
      throw new IllegalStateException("Invalid number of leading ones in UTF encoded integer");
    } else if (leadingOnes == 0) {
      return firstByte;
    }

    long value = firstByte - (1L << (7 - leadingOnes)) - 1L;

    for (int i = 0; i < leadingOnes - 1; i++) {
      int currentByte = bitStreamReader.asInteger(8);
      if ((currentByte & 0xC0) != 0x80) {
        throw new IllegalStateException("Invalid content of payload byte, first bits must be 1 and 0.");
      }

      value = (value << 6) | (currentByte & 0x3F);
    }

    return value;
  }
}
