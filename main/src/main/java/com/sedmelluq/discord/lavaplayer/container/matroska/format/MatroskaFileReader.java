package com.sedmelluq.discord.lavaplayer.container.matroska.format;

import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Handles reading of elements and their content from an MKV file.
 */
public class MatroskaFileReader {
  private final SeekableInputStream inputStream;
  private final DataInput dataInput;
  private final MutableMatroskaElement[] levels;
  private final MutableMatroskaBlock mutableBlock;

  /**
   * @param inputStream Input stream to read from.
   */
  public MatroskaFileReader(SeekableInputStream inputStream) {
    this.inputStream = inputStream;
    this.dataInput = new DataInputStream(inputStream);
    this.levels = new MutableMatroskaElement[8];
    this.mutableBlock = new MutableMatroskaBlock();
  }

  /**
   * @param parent The parent element to use for bounds checking, null is valid.
   * @return The element whose header was read. Null if the parent/file has ended. The contents of this element are only
   *         valid until the next element at the same level is read, use {@link MatroskaElement#frozen()} to get a
   *         permanent instance.
   * @throws IOException On read error
   */
  public MatroskaElement readNextElement(MatroskaElement parent) throws IOException {
    long position = inputStream.getPosition();
    long remaining = parent != null ? parent.getRemaining(position) : inputStream.getContentLength() - position;

    if (remaining == 0) {
      return null;
    } else if (remaining < 0) {
      throw new IllegalStateException("Current position is beyond this element");
    }

    long id = MatroskaEbmlReader.readEbmlInteger(dataInput, null);
    long dataSize = MatroskaEbmlReader.readEbmlInteger(dataInput, null);
    long dataPosition = inputStream.getPosition();

    int level = parent == null ? 0 : parent.getLevel() + 1;
    MutableMatroskaElement element = levels[level];

    if (element == null) {
      element = levels[level] = new MutableMatroskaElement(level);
    }

    element.setId(id);
    element.setType(MatroskaElementType.find(id));
    element.setPosition(position);
    element.setHeaderSize((int) (dataPosition - position));
    element.setDataSize((int) dataSize);
    return element;
  }

  /**
   * Reads one Matroska block header. The data is of the block is not read, but can be read frame by frame using
   * {@link MatroskaBlock#getNextFrameBuffer(MatroskaFileReader, int)}.
   *
   * @param parent The block parent element.
   * @param trackFilter The ID of the track to read data for from the block.
   * @return An instance of a block if it contains data for the requested track, <code>null</code> otherwise.
   * @throws IOException On read error.
   */
  public MatroskaBlock readBlockHeader(MatroskaElement parent, int trackFilter) throws IOException {
    if (!mutableBlock.parseHeader(this, parent, trackFilter)) {
      return null;
    }

    return mutableBlock;
  }

  /**
   * @param element Element to read from
   * @return The contents of the element as an integer
   * @throws IOException On read error
   */
  public int asInteger(MatroskaElement element) throws IOException {
    if (element.is(MatroskaElementType.DataType.UNSIGNED_INTEGER)) {
      long value = MatroskaEbmlReader.readFixedSizeEbmlInteger(dataInput, element.dataSize, null);

      if (value < 0 || value > Integer.MAX_VALUE) {
        throw new ArithmeticException("Cannot convert unsigned value to integer.");
      } else {
        return (int) value;
      }
    } else if (element.is(MatroskaElementType.DataType.SIGNED_INTEGER)) {
      return Math.toIntExact(MatroskaEbmlReader.readFixedSizeEbmlInteger(dataInput, element.dataSize, MatroskaEbmlReader.Type.SIGNED));
    } else {
      throw new IllegalArgumentException("Not an integer element.");
    }
  }

  /**
   * @param element Element to read from
   * @return The contents of the element as a long
   * @throws IOException On read error
   */
  public long asLong(MatroskaElement element) throws IOException {
    if (element.is(MatroskaElementType.DataType.UNSIGNED_INTEGER)) {
      return MatroskaEbmlReader.readFixedSizeEbmlInteger(dataInput, element.dataSize, null);
    } else if (element.is(MatroskaElementType.DataType.SIGNED_INTEGER)) {
      return MatroskaEbmlReader.readFixedSizeEbmlInteger(dataInput, element.dataSize, MatroskaEbmlReader.Type.SIGNED);
    } else {
      throw new IllegalArgumentException("Not an integer element.");
    }
  }

  /**
   * @param element Element to read from
   * @return The contents of the element as a float
   * @throws IOException On read error
   */
  public float asFloat(MatroskaElement element) throws IOException {
    if (element.is(MatroskaElementType.DataType.FLOAT)) {
      if (element.dataSize == 4) {
        return dataInput.readFloat();
      } else if (element.dataSize == 8) {
        return (float) dataInput.readDouble();
      } else {
        throw new IllegalStateException("Float element has invalid size.");
      }
    } else {
      throw new IllegalArgumentException("Not a float element.");
    }
  }

  /**
   * @param element Element to read from
   * @return The contents of the element as a double
   * @throws IOException On read error
   */
  public double asDouble(MatroskaElement element) throws IOException {
    if (element.is(MatroskaElementType.DataType.FLOAT)) {
      if (element.dataSize == 4) {
        return dataInput.readFloat();
      } else if (element.dataSize == 8) {
        return dataInput.readDouble();
      } else {
        throw new IllegalStateException("Float element has invalid size.");
      }
    } else {
      throw new IllegalArgumentException("Not a float element.");
    }
  }

  /**
   * @param element Element to read from
   * @return The contents of the element as a string
   * @throws IOException On read error
   */
  public String asString(MatroskaElement element) throws IOException {
    if (element.is(MatroskaElementType.DataType.STRING)) {
      return new String(asBytes(element), StandardCharsets.US_ASCII);
    } else if (element.is(MatroskaElementType.DataType.UTF8_STRING)) {
      return new String(asBytes(element), StandardCharsets.UTF_8);
    } else {
      throw new IllegalArgumentException("Not a string element.");
    }
  }

  /**
   * @param element Element to read from
   * @return The contents of the element as a byte array
   * @throws IOException On read error
   */
  public byte[] asBytes(MatroskaElement element) throws IOException {
    byte[] bytes = new byte[element.dataSize];
    dataInput.readFully(bytes);
    return bytes;
  }

  /**
   * @param element Element to skip over
   * @throws IOException On read error
   */
  public void skip(MatroskaElement element) throws IOException {
    long remaining = element.getRemaining(inputStream.getPosition());

    if (remaining > 0) {
      inputStream.skipFully(remaining);
    } else if (remaining < 0) {
      throw new IllegalStateException("Current position is beyond this element");
    }
  }

  /**
   * @return Returns the current absolute position of the file.
   */
  public long getPosition() {
    return inputStream.getPosition();
  }

  /**
   * Seeks to the specified position.
   * @param position The position in bytes.
   * @throws IOException On read error
   */
  public void seek(long position) throws IOException {
    inputStream.seek(position);
  }

  public DataInput getDataInput() {
    return dataInput;
  }
}
