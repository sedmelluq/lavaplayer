package com.sedmelluq.discord.lavaplayer.container.matroska.format;

/**
 * Mutable instance of {@link MatroskaElement} for reducing allocation rate during parsing.
 */
public class MutableMatroskaElement extends MatroskaElement {
  protected MutableMatroskaElement(int level) {
    super(level);
  }
  
  public void setId(long id) {
    this.id = id;
  }

  public void setType(MatroskaElementType type) {
    this.type = type;
  }

  public void setPosition(long position) {
    this.position = position;
  }

  public void setHeaderSize(int headerSize) {
    this.headerSize = headerSize;
  }

  public void setDataSize(int dataSize) {
    this.dataSize = dataSize;
  }
}
