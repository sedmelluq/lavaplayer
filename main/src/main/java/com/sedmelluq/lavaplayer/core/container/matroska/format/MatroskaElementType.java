package com.sedmelluq.lavaplayer.core.container.matroska.format;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.sedmelluq.lavaplayer.core.container.matroska.format.MatroskaElementType.DataType.BINARY;
import static com.sedmelluq.lavaplayer.core.container.matroska.format.MatroskaElementType.DataType.DATE;
import static com.sedmelluq.lavaplayer.core.container.matroska.format.MatroskaElementType.DataType.FLOAT;
import static com.sedmelluq.lavaplayer.core.container.matroska.format.MatroskaElementType.DataType.MASTER;
import static com.sedmelluq.lavaplayer.core.container.matroska.format.MatroskaElementType.DataType.SIGNED_INTEGER;
import static com.sedmelluq.lavaplayer.core.container.matroska.format.MatroskaElementType.DataType.STRING;
import static com.sedmelluq.lavaplayer.core.container.matroska.format.MatroskaElementType.DataType.UNSIGNED_INTEGER;
import static com.sedmelluq.lavaplayer.core.container.matroska.format.MatroskaElementType.DataType.UTF8_STRING;

/**
 * Registry of all required element types. This is not the complete set.
 */
@SuppressWarnings("squid:S00115")
public enum MatroskaElementType {
  Ebml(MASTER, new int[] { 0x1A, 0x45, 0xDF, 0xA3 }),
  EbmlVersion(UNSIGNED_INTEGER, new int[] { 0x42, 0x86 }),
  EbmlReadVersion(UNSIGNED_INTEGER, new int[] { 0x42, 0xF7 }),
  EbmlMaxIdLength(UNSIGNED_INTEGER, new int[] { 0x42, 0xF2 }),
  EbmlMaxSizeLength(UNSIGNED_INTEGER, new int[] { 0x42, 0xF3 }),
  DocType(STRING, new int[] { 0x42, 0x82 }),
  DocTypeVersion(UNSIGNED_INTEGER, new int[] { 0x42, 0x87 }),
  DocTypeReadVersion(UNSIGNED_INTEGER, new int[] { 0x42, 0x85 }),
  Void(BINARY, new int[] { 0xEC }),
  Crc32(BINARY, new int[] { 0xBF }),
  SignatureSlot(MASTER, new int[] { 0x1B, 0x53, 0x86, 0x67 }),
  SignatureAlgo(UNSIGNED_INTEGER, new int[] { 0x7E, 0x8A }),
  SignatureHash(UNSIGNED_INTEGER, new int[] { 0x7E, 0x9A }),
  SignaturePublicKey(BINARY, new int[] { 0x7E, 0xA5 }),
  Signature(BINARY, new int[] { 0x7E, 0xB5 }),
  SignatureElements(MASTER, new int[] { 0x7E, 0x5B }),
  SignatureElementList(MASTER, new int[] { 0x7E, 0x7B }),
  SignedElement(BINARY, new int[] { 0x65, 0x32 }),
  Segment(MASTER, new int[] { 0x18, 0x53, 0x80, 0x67 }),
  SeekHead(MASTER, new int[] { 0x11, 0x4D, 0x9B, 0x74 }),
  Seek(MASTER, new int[] { 0x4D, 0xBB }),
  SeekId(BINARY, new int[] { 0x53, 0xAB }),
  SeekPosition(UNSIGNED_INTEGER, new int[] { 0x53, 0xAC }),
  Info(MASTER, new int[] { 0x15, 0x49, 0xA9, 0x66 }),
  SegmentUid(BINARY, new int[] { 0x73, 0xA4 }),
  SegmentFilename(UTF8_STRING, new int[] { 0x73, 0x84 }),
  PrevUid(BINARY, new int[] { 0x3C, 0xB9, 0x23 }),
  PrevFilename(UTF8_STRING, new int[] { 0x3C, 0x83, 0xAB }),
  NextUid(BINARY, new int[] { 0x3E, 0xB9, 0x23 }),
  NextFilename(UTF8_STRING, new int[] { 0x3E, 0x83, 0xBB }),
  SegmentFamily(BINARY, new int[] { 0x44, 0x44 }),
  ChapterTranslate(MASTER, new int[] { 0x69, 0x24 }),
  TimecodeScale(UNSIGNED_INTEGER, new int[] { 0x2A, 0xD7, 0xB1 }),
  Duration(FLOAT, new int[] { 0x44, 0x89 }),
  DateUtc(DATE, new int[] { 0x44, 0x61 }),
  Title(UTF8_STRING, new int[] { 0x7B, 0xA9 }),
  MuxingApp(UTF8_STRING, new int[] { 0x4D, 0x80 }),
  WritingApp(UTF8_STRING, new int[] { 0x57, 0x41 }),
  Cluster(MASTER, new int[] { 0x1F, 0x43, 0xB6, 0x75 }),
  Timecode(UNSIGNED_INTEGER, new int[] { 0xE7 }),
  SimpleBlock(BINARY, new int[] { 0xA3 }),
  BlockGroup(MASTER, new int[] { 0xA0 }),
  Block(BINARY, new int[] { 0xA1 }),
  BlockVirtual(BINARY, new int[] { 0xA2 }),
  BlockAdditions(MASTER, new int[] { 0x75, 0xA1 }),
  BlockMore(MASTER, new int[] { 0xA6 }),
  BlockAddId(UNSIGNED_INTEGER, new int[] { 0xEE }),
  BlockAdditional(BINARY, new int[] { 0xA5 }),
  BlockDuration(UNSIGNED_INTEGER, new int[] { 0x9B }),
  ReferencePriority(UNSIGNED_INTEGER, new int[] { 0xFA }),
  ReferenceBlock(SIGNED_INTEGER, new int[] { 0xFB }),
  ReferenceVirtual(SIGNED_INTEGER, new int[] { 0xFD }),
  CodecState(BINARY, new int[] { 0xA4 }),
  DiscardPadding(SIGNED_INTEGER, new int[] { 0x75, 0xA2 }),
  Slices(MASTER, new int[] { 0x8E }),
  Tracks(MASTER, new int[] { 0x16, 0x54, 0xAE, 0x6B }),
  TrackEntry(MASTER, new int[] { 0xAE }),
  TrackNumber(UNSIGNED_INTEGER, new int[] { 0xD7 }),
  TrackUid(UNSIGNED_INTEGER, new int[] { 0x73, 0xC5 }),
  TrackType(UNSIGNED_INTEGER, new int[] { 0x83 }),
  FlagEnabled(UNSIGNED_INTEGER, new int[] { 0xB9 }),
  FlagDefault(UNSIGNED_INTEGER, new int[] { 0x88 }),
  FlagForced(UNSIGNED_INTEGER, new int[] { 0x55, 0xAA }),
  FlagLacing(UNSIGNED_INTEGER, new int[] { 0x9C }),
  MinCache(UNSIGNED_INTEGER, new int[] { 0x6D, 0xE7 }),
  MaxCache(UNSIGNED_INTEGER, new int[] { 0x6D, 0xF8 }),
  DefaultDuration(UNSIGNED_INTEGER, new int[] { 0x23, 0xE3, 0x83 }),
  DefaultDecodedFieldDuration(UNSIGNED_INTEGER, new int[] { 0x23, 0x4E, 0x7A }),
  TrackTimecodeScale(FLOAT, new int[] { 0x23, 0x31, 0x4F }),
  TrackOffset(SIGNED_INTEGER, new int[] { 0x53, 0x7F }),
  MaxBlockAdditionId(UNSIGNED_INTEGER, new int[] { 0x55, 0xEE }),
  Name(UTF8_STRING, new int[] { 0x53, 0x6E }),
  Language(STRING, new int[] { 0x22, 0xB5, 0x9C }),
  LanguageIetf(STRING, new int[] { 0x22, 0xB5, 0x9D }),
  CodecId(STRING, new int[] { 0x86 }),
  CodecPrivate(BINARY, new int[] { 0x63, 0xA2 }),
  CodecName(UTF8_STRING, new int[] { 0x25, 0x86, 0x88 }),
  AttachmentLink(UNSIGNED_INTEGER, new int[] { 0x74, 0x46 }),
  CodecSettings(UTF8_STRING, new int[] { 0x3A, 0x96, 0x97 }),
  CodecInfoUrl(STRING, new int[] { 0x3B, 0x40, 0x40 }),
  CodecDownloadUrl(STRING, new int[] { 0x26, 0xB2, 0x40 }),
  CodecDecodeAll(UNSIGNED_INTEGER, new int[] { 0xAA }),
  TrackOverlay(UNSIGNED_INTEGER, new int[] { 0x6F, 0xAB }),
  CodecDelay(UNSIGNED_INTEGER, new int[] { 0x56, 0xAA }),
  SeekPreRoll(UNSIGNED_INTEGER, new int[] { 0x56, 0xBB }),
  TrackTranslate(MASTER, new int[] { 0x66, 0x24 }),
  TrackTranslateEditionUid(UNSIGNED_INTEGER, new int[] { 0x66, 0xFC }),
  TrackTranslateCodec(UNSIGNED_INTEGER, new int[] { 0x66, 0xBF }),
  TrackTranslateTrackId(BINARY, new int[] { 0x66, 0xA5 }),
  Video(MASTER, new int[] { 0xE0 }),
  Audio(MASTER, new int[] { 0xE1 }),
  SamplingFrequency(FLOAT, new int[] { 0xB5 }),
  OutputSamplingFrequency(FLOAT, new int[] { 0x78, 0xB5 }),
  Channels(UNSIGNED_INTEGER, new int[] { 0x9F }),
  BitDepth(UNSIGNED_INTEGER, new int[] { 0x62, 0x64 }),
  TrackOperation(MASTER, new int[] { 0xE2 }),
  TrickTrackUid(UNSIGNED_INTEGER, new int[] { 0xC0 }),
  TrickTrackSegmentUid(BINARY, new int[] { 0xC1 }),
  TrickTrackFlag(UNSIGNED_INTEGER, new int[] { 0xC6 }),
  TrickTrackMasterTrackUid(UNSIGNED_INTEGER, new int[] { 0xC7 }),
  TrickMasterTrackSegmentUid(BINARY, new int[] { 0xC4 }),
  ContentEncodings(MASTER, new int[] { 0x6D, 0x80 }),
  Cues(MASTER, new int[] { 0x1C, 0x53, 0xBB, 0x6B }),
  CuePoint(MASTER, new int[] { 0xBB }),
  CueTime(UNSIGNED_INTEGER, new int[] { 0xB3 }),
  CueTrackPositions(MASTER, new int[] { 0xB7 }),
  CueTrack(UNSIGNED_INTEGER, new int[] { 0xF7 }),
  CueClusterPosition(UNSIGNED_INTEGER, new int[] { 0xF1 }),
  CueRelativePosition(UNSIGNED_INTEGER, new int[] { 0xF0 }),
  CueDuration(UNSIGNED_INTEGER, new int[] { 0xB2 }),
  CueBlockNumber(UNSIGNED_INTEGER, new int[] { 0x53, 0x78 }),
  CueCodecState(UNSIGNED_INTEGER, new int[] { 0xEA }),
  CueReference(MASTER, new int[] { 0xDB }),
  CueRefTime(UNSIGNED_INTEGER, new int[] { 0x96 }),
  CueRefCluster(UNSIGNED_INTEGER, new int[] { 0x97 }),
  CueRefNumber(UNSIGNED_INTEGER, new int[] { 0x53, 0x5F }),
  CueRefCodecState(UNSIGNED_INTEGER, new int[] { 0xEB }),
  Attachments(MASTER, new int[] { 0x19, 0x41, 0xA4, 0x69 }),
  Chapters(MASTER, new int[] { 0x10, 0x43, 0xA7, 0x70 }),
  Tags(MASTER, new int[] { 0x12, 0x54, 0xC3, 0x67 }),
  Tag(MASTER, new int[] { 0x73, 0x73 }),
  Targets(MASTER, new int[] { 0x63, 0xC0 }),
  TargetTypeValue(UNSIGNED_INTEGER, new int[] { 0x68, 0xCA }),
  TargetType(STRING, new int[] { 0x63, 0xCA }),
  TagTrackUid(UNSIGNED_INTEGER, new int[] { 0x63, 0xC5 }),
  TagEditionUid(UNSIGNED_INTEGER, new int[] { 0x63, 0xC9 }),
  TagChapterUid(UNSIGNED_INTEGER, new int[] { 0x63, 0xC4 }),
  TagAttachmentUid(UNSIGNED_INTEGER, new int[] { 0x63, 0xC6 }),
  SimpleTag(MASTER, new int[] { 0x67, 0xC8 }),
  TagName(UTF8_STRING, new int[] { 0x45, 0xA3 }),
  TagLanguage(STRING, new int[] { 0x44, 0x7A }),
  TagLanguageIetf(STRING, new int[] { 0x44, 0x7B }),
  TagDefault(UNSIGNED_INTEGER, new int[] { 0x44, 0x84 }),
  TagString(UTF8_STRING, new int[] { 0x44, 0x87 }),
  TagBinary(BINARY, new int[] { 0x44, 0x85 }),
  Unknown(BINARY, new int[] { });

  private static final Map<Long, MatroskaElementType> mapping;

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
