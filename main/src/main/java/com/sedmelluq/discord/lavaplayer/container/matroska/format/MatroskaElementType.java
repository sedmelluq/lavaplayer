package com.sedmelluq.discord.lavaplayer.container.matroska.format;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry of all required element types. This is not the complete set.
 */
@SuppressWarnings("squid:S00115")
public enum MatroskaElementType {
  Ebml(DataType.MASTER, new int[] { 0x1A, 0x45, 0xDF, 0xA3 }),
  DocType(DataType.STRING, new int[] { 0x42, 0x82 }),
  Segment(DataType.MASTER, new int[] { 0x18, 0x53, 0x80, 0x67 }),
  SeekHead(DataType.MASTER, new int[] { 0x11, 0x4D, 0x9B, 0x74 }),
  Seek(DataType.MASTER, new int[] { 0x4D, 0xBB }),
  SeekId(DataType.BINARY, new int[] { 0x53, 0xAB }),
  SeekPosition(DataType.UNSIGNED_INTEGER, new int[] { 0x53, 0xAC }),
  Info(DataType.MASTER, new int[] { 0x15, 0x49, 0xA9, 0x66 }),
  Duration(DataType.FLOAT, new int[] { 0x44, 0x89 }),
  TimecodeScale(DataType.UNSIGNED_INTEGER, new int[] { 0x2A, 0xD7, 0xB1 }),
  Cluster(DataType.MASTER, new int[] { 0x1F, 0x43, 0xB6, 0x75 }),
  Timecode(DataType.UNSIGNED_INTEGER, new int[] { 0xE7 }),
  SimpleBlock(DataType.BINARY, new int[] { 0xA3 }),
  BlockGroup(DataType.MASTER, new int[] { 0xA0 }),
  Block(DataType.BINARY, new int[] { 0xA1 }),
  BlockDuration(DataType.UNSIGNED_INTEGER, new int[] { 0x9B }),
  ReferenceBlock(DataType.SIGNED_INTEGER, new int[] { 0xFB }),
  Tracks(DataType.MASTER, new int[] { 0x16, 0x54, 0xAE, 0x6B }),
  TrackEntry(DataType.MASTER, new int[] { 0xAE }),
  TrackNumber(DataType.UNSIGNED_INTEGER, new int[] { 0xD7 }),
  TrackUid(DataType.UNSIGNED_INTEGER, new int[] { 0x73, 0xC5 }),
  TrackType(DataType.UNSIGNED_INTEGER, new int[] { 0x83 }),
  Name(DataType.UTF8_STRING, new int[] { 0x53, 0x6E }),
  CodecId(DataType.STRING, new int[] { 0x86 }),
  CodecPrivate(DataType.BINARY, new int[] { 0x63, 0xA2 }),
  Audio(DataType.MASTER, new int[] { 0xE1 }),
  SamplingFrequency(DataType.FLOAT, new int[] { 0xB5 }),
  OutputSamplingFrequency(DataType.FLOAT, new int[] { 0x78, 0xB5 }),
  Channels(DataType.UNSIGNED_INTEGER, new int[] { 0x9F }),
  BitDepth(DataType.UNSIGNED_INTEGER, new int[] { 0x62, 0x64 }),
  Cues(DataType.MASTER, new int[] { 0x1C, 0x53, 0xBB, 0x6B }),
  CuePoint(DataType.MASTER, new int[] { 0xBB }),
  CueTime(DataType.UNSIGNED_INTEGER, new int[] { 0xB3 }),
  CueTrackPositions(DataType.MASTER, new int[] { 0xB7 }),
  CueTrack(DataType.UNSIGNED_INTEGER, new int[] { 0xF7 }),
  CueClusterPosition(DataType.UNSIGNED_INTEGER, new int[] { 0xF1 }),
  Unknown(DataType.BINARY, new int[] { });

  private static Map<Long, MatroskaElementType> mapping;

  /**
   * The ID as EBML code bytes.
   */
  public final byte[] bytes;
  /**
   * The ID of the element type.
   */
  public final long id;
  /**
   * Data type of the element type.
   */
  public final DataType dataType;

  static {
    Map<Long, MatroskaElementType> codeMapping = new HashMap<>();

    for (MatroskaElementType code : MatroskaElementType.class.getEnumConstants()) {
      if (code != Unknown) {
        codeMapping.put(code.id, code);
      }
    }

    mapping = codeMapping;
  }

  MatroskaElementType(DataType dataType, int[] integers) {
    this.dataType = dataType;
    this.bytes = asByteArray(integers);
    this.id = bytes.length > 0 ? MatroskaEbmlReader.readEbmlInteger(ByteBuffer.wrap(bytes), null) : -1;
  }

  /**
   * Data type of an element.
   */
  public enum DataType {
    /**
     * Contains child elements.
     */
    MASTER,
    /**
     * Unsigned EBML integer.
     */
    UNSIGNED_INTEGER,
    /**
     * Signed EBML integer.
     */
    SIGNED_INTEGER,
    /**
     * ASCII-encoded string.
     */
    STRING,
    /**
     * UTF8-encoded string.
     */
    UTF8_STRING,
    /**
     * Raw binary data.
     */
    BINARY,
    /**
     * Float (either 4 or 8 byte)
     */
    FLOAT,
    /**
     * Nanosecond timestamp since 2001.
     */
    DATE
  }

  private static byte[] asByteArray(int[] integers) {
    byte[] bytes = new byte[integers.length];

    for (int i = 0; i < integers.length; i++) {
      bytes[i] = (byte) integers[i];
    }

    return bytes;
  }

  /**
   * @param id Code of the element type to find
   * @return The element type, Unknown if not present.
   */
  public static MatroskaElementType find(long id) {
    MatroskaElementType code = mapping.get(id);
    if (code == null) {
      code = Unknown;
    }
    return code;
  }
}
