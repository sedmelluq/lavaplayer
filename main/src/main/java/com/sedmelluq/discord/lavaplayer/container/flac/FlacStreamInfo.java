package com.sedmelluq.discord.lavaplayer.container.flac;

import com.sedmelluq.discord.lavaplayer.tools.io.BitBufferReader;

import java.nio.ByteBuffer;

/**
 * FLAC stream info metadata block contents. Field descriptions are from:
 * https://xiph.org/flac/format.html#metadata_block_streaminfo
 *
 * FLAC specifies a minimum block size of 16 and a maximum block size of 65535, meaning the bit patterns corresponding
 * to the numbers 0-15 in the minimum blocksize and maximum blocksize fields are invalid.
 */
public class FlacStreamInfo {
  public static final int LENGTH = 34;

  /**
   * The minimum block size (in samples) used in the stream.
   */
  public final int minimumBlockSize;
  /**
   * The maximum block size (in samples) used in the stream. (Minimum blocksize == maximum blocksize) implies a
   * fixed-blocksize stream.
   */
  public final int maximumBlockSize;
  /**
   * The minimum frame size (in bytes) used in the stream. May be 0 to imply the value is not known.
   */
  public final int minimumFrameSize;
  /**
   * The maximum frame size (in bytes) used in the stream. May be 0 to imply the value is not known.
   */
  public final int maximumFrameSize;
  /**
   * Sample rate in Hz. Though 20 bits are available, the maximum sample rate is limited by the structure of frame
   * headers to 655350Hz. Also, a value of 0 is invalid.
   */
  public final int sampleRate;
  /**
   * FLAC supports from 1 to 8 channels
   */
  public final int channelCount;
  /**
   * FLAC supports from 4 to 32 bits per sample. Currently the reference encoder and decoders only support up to 24 bits
   * per sample.
   */
  public final int bitsPerSample;
  /**
   * Total samples in stream. 'Samples' means inter-channel sample, i.e. one second of 44.1Khz audio will have 44100
   * samples regardless of the number of channels. A value of zero here means the number of total samples is unknown.
   */
  public final long sampleCount;
  /**
   * MD5 signature of the unencoded audio data. This allows the decoder to determine if an error exists in the audio
   * data even when the error does not result in an invalid bitstream.
   */
  public final byte[] md5Signature;
  /**
   * Whether the file has any metadata blocks after the stream info.
   */
  public final boolean hasMetadataBlocks;

  /**
   * @param blockData The raw block data.
   * @param hasMetadataBlocks Whether the file has any metadata blocks after the stream info.
   */
  public FlacStreamInfo(byte[] blockData, boolean hasMetadataBlocks) {
    BitBufferReader bitReader = new BitBufferReader(ByteBuffer.wrap(blockData));
    minimumBlockSize = bitReader.asInteger(16);
    maximumBlockSize = bitReader.asInteger(16);
    minimumFrameSize = bitReader.asInteger(24);
    maximumFrameSize = bitReader.asInteger(24);
    sampleRate = bitReader.asInteger(20);
    channelCount = bitReader.asInteger(3) + 1;
    bitsPerSample = bitReader.asInteger(5) + 1;
    sampleCount = bitReader.asLong(36);

    md5Signature = new byte[16];
    System.arraycopy(blockData, 18, md5Signature, 0, 16);

    this.hasMetadataBlocks = hasMetadataBlocks;
  }
}
