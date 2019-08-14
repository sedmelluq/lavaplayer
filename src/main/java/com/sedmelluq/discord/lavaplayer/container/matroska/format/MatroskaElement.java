package com.sedmelluq.discord.lavaplayer.container.matroska.format;

/**
 * Matroska container element.
 */
public class MatroskaElement {
  protected final int level;
  protected long id;
  protected MatroskaElementType type;
  protected long position;
  protected int headerSize;
  protected int dataSize;

  protected MatroskaElement(int level) {
    this.level = level;
  }

  /**
   * @return The depth of the element in the element tree.
   */
  public int getLevel() {
    return level;
  }

  /**
   * @return The EBML code of the element.
   */
  public long getId() {
    return id;
  }

  /**
   * @return Element type, Unknown if not listed in the enum.
   */
  public MatroskaElementType getType() {
    return type;
  }

  /**
   * @return Absolute position of the element in the file.
   */
  public long getPosition() {
    return position;
  }

  /**
   * @return Size of the header in bytes.
   */
  public int getHeaderSize() {
    return headerSize;
  }

  /**
   * @return Size of the payload in bytes.
   */
  public int getDataSize() {
    return dataSize;
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

  /**
   * @return A frozen version of the element safe to keep for later use.
   */
  public MatroskaElement frozen() {
    MatroskaElement element = new MatroskaElement(level);
    element.id = id;
    element.type = type;
    element.position = position;
    element.headerSize = headerSize;
    element.dataSize = dataSize;
    return element;
  }
}
