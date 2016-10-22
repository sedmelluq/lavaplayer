package com.sedmelluq.discord.lavaplayer.container.flac;

import com.sedmelluq.discord.lavaplayer.tools.io.BitStreamReader;

import java.io.IOException;

/**
 * Contains methods for reading a frame header.
 */
public class FlacFrameHeaderReader {
  private static final int VALUE_INVALID = Integer.MIN_VALUE;
  private static final int VALUE_INHERITED = 0;

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

  /**
   * Reads a frame header. At this point the first two bytes of the frame have actually been read during the frame sync
   * scanning already. This means that this method expects there to be no EOF in the middle of the header. The frame
   * information must match that of the stream, as changing sample rates, channel counts and sample sizes are not
   * supported.
   *
   * @param bitStreamReader Bit stream reader for input
   * @param streamInfo Information about the stream from metadata headers
   * @return The frame information.
   * @throws IOException
   */
  public static FlacFrameInfo readFrameHeader(BitStreamReader bitStreamReader, FlacStreamInfo streamInfo) throws IOException {
    int blockSize = blockSizeMapping[bitStreamReader.asInteger(4)];
    int sampleRate = sampleRateMapping[bitStreamReader.asInteger(4)];

    // TODO: Finish this

    return null;
  }
}
