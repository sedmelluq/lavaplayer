package com.sedmelluq.discord.lavaplayer.container.matroska.format;

/**
 * Matroska container element.
 */
public class MatroskaElement {
  /**
   * The EBML code of the element.
   */
  public final long id;
  /**
   * Element type, Unknown if not listed in the enum.
   */
  public final MatroskaElementType type;
  /**
   * Absolute position of the element in the file.
   */
  public final long position;
  /**
   * Size of the header in bytes.
   */
  public final int headerSize;
  /**
   * Size of the payload in bytes.
   */
  public final int dataSize;

  /**
   * @param id The EBML code of the element.
   * @param type Element type, Unknown if not listed in the enum.
   * @param position Absolute position of the element in the file.
   * @param headerSize Size of the header in bytes.
   * @param dataSize Size of the data in bytes.
   */
  public MatroskaElement(long id, MatroskaElementType type, long position, int headerSize, int dataSize) {
    this.id = id;
    this.type = type;
    this.position = position;
    this.headerSize = headerSize;
    this.dataSize = dataSize;
  }

  /**
   * @param type Element type.
   * @return True if this element is of the specified type.
   */
  public boolean is(MatroskaElementType type) {
    return type.id == this.id;
  }

  /**
   * @param dataType Element data type.
   * @return True if the type of the element uses the specified data type.
   */
  public boolean is(MatroskaElementType.DataType dataType) {
    return dataType == type.dataType;
  }

  /**
   * @param currentPosition Absolute position to check against.
   * @return The number of bytes from the specified position to the end of this element.
   */
  public long getRemaining(long currentPosition) {
    return (position + headerSize + dataSize) - currentPosition;
  }

  /**
   * @return The absolute position of the data of this element.
   */
  public long getDataPosition() {
    return position + headerSize;
  }
}
