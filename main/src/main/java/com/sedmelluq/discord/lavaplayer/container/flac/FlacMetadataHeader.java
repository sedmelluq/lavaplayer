package com.sedmelluq.discord.lavaplayer.container.flac;

import com.sedmelluq.discord.lavaplayer.tools.io.BitBufferReader;

import java.nio.ByteBuffer;

/**
 * A header of FLAC metadata.
 */
public class FlacMetadataHeader {
  public static final int LENGTH = 4;

  public static final int BLOCK_SEEKTABLE = 3;
  public static final int BLOCK_COMMENT = 4;

  /**
   * If this header is for the last metadata block. If this is true, then the current metadata block is followed by
   * frames.
   */
  public final boolean isLastBlock;

  /**
   * Block type, see: https://xiph.org/flac/format.html#metadata_block_header
   */
  public final int blockType;

  /**
   * Length of the block, current header excluded
   */
  public final int blockLength;

  /**
   * @param data The raw header data
   */
  public FlacMetadataHeader(byte[] data) {
    BitBufferReader bitReader = new BitBufferReader(ByteBuffer.wrap(data));
    isLastBlock = bitReader.asInteger(1) == 1;
    blockType = bitReader.asInteger(7);
    blockLength = bitReader.asInteger(24);
  }
}
