package com.sedmelluq.discord.lavaplayer.container.mpeg.reader;

import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles reading parts of an MP4 file
 */
public class MpegReader {
  /**
   * The input as a DataInput
   */
  public final DataInput data;

  /**
   * The input as a seekable stream
   */
  public final SeekableInputStream seek;

  private final byte[] fourCcBuffer;
  private final ByteBuffer readAttemptBuffer;

  /**
   * @param inputStream Input as a seekable stream
   */
  public MpegReader(SeekableInputStream inputStream) {
    seek = inputStream;
    data = new DataInputStream(inputStream);
    fourCcBuffer = new byte[4];
    readAttemptBuffer = ByteBuffer.allocate(4);
  }

  /**
   * Reads the header of the next child element. Assumes position is at the start of a header or at the end of the section.
   * @param parent The section from which to read child sections from
   * @return The element if there were any more child elements
   */
  public MpegSectionInfo nextChild(MpegSectionInfo parent) {
    if (parent.offset + parent.length <= seek.getPosition() + 8) {
      return null;
    }

    try {
      long offset = seek.getPosition();
      Integer lengthField = tryReadInt();

      if (lengthField == null) {
        return null;
      }

      long length = Integer.toUnsignedLong(lengthField);
      return new MpegSectionInfo(offset, length, readFourCC());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Skip to the end of a section.
   * @param section The section to skip
   */
  public void skip(MpegSectionInfo section) {
    try {
      seek.seek(section.offset + section.length);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Read a FourCC as a string
   * @return The FourCC string
   * @throws IOException When reading the bytes from input fails
   */
  public String readFourCC() throws IOException {
    data.readFully(fourCcBuffer);
    return new String(fourCcBuffer, "ISO-8859-1");
  }

  /**
   * Read an UTF string with a specified size.
   * @param size Size in bytes.
   * @return The string read from the stream
   * @throws IOException On read error
   */
  public String readUtfString(int size) throws IOException {
    byte[] bytes = new byte[size];
    data.readFully(bytes);

    return new String(bytes, StandardCharsets.UTF_8);
  }

  /**
   * Read a null-terminated UTF string.
   * @return The string read from the stream
   * @throws IOException On read error
   */
  public String readTerminatedString() throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    byte nextByte;

    while ((nextByte = data.readByte()) != 0) {
      bytes.write(nextByte);
    }

    return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
  }

  /**
   * Parse the flags and version for the specified section
   * @param section The section where the flags and version should be parsed
   * @return The section info with version info
   * @throws IOException On a read error
   */
  public MpegVersionedSectionInfo parseFlags(MpegSectionInfo section) throws IOException {
    return parseFlagsForSection(data, section);
  }

  private static MpegVersionedSectionInfo parseFlagsForSection(DataInput in, MpegSectionInfo section) throws IOException {
    int versionAndFlags = in.readInt();
    return new MpegVersionedSectionInfo(section, versionAndFlags >>> 24, versionAndFlags & 0xffffff);
  }

  private Integer tryReadInt() throws IOException {
    int firstByte = seek.read();

    if (firstByte == -1) {
      return null;
    }

    readAttemptBuffer.put(0, (byte) firstByte);
    data.readFully(readAttemptBuffer.array(), 1, 3);
    return readAttemptBuffer.getInt(0);
  }

  /**
   * Start a child element handling chain
   * @param parent The parent chain
   * @return The chain
   */
  public Chain in(MpegSectionInfo parent) {
    return new Chain(parent, this);
  }

  /**
   * Child element processing helper class.
   */
  public static class Chain {
    private final MpegSectionInfo parent;
    private final List<Handler> handlers;
    private final MpegReader reader;
    private MpegParseStopChecker stopChecker;

    private Chain(MpegSectionInfo parent, MpegReader reader) {
      this.parent = parent;
      this.reader = reader;
      handlers = new ArrayList<>();
    }

    /**
     * @param type The FourCC of the section for which a handler is specified
     * @param handler The handler
     * @return this
     */
    public Chain handle(String type, MpegSectionHandler handler) {
      handle(type, false, handler);
      return this;
    }

    /**
     * @param type The FourCC of the section for which a handler is specified
     * @param finish Whether to stop reading after this section
     * @param handler The handler
     * @return this
     */
    public Chain handle(String type, boolean finish, MpegSectionHandler handler) {
      handlers.add(new Handler(type, finish, handler));
      return this;
    }

    /**
     * @param type The FourCC of the section for which a handler is specified
     * @param handler The handler which expects versioned section info
     * @return this
     */
    public Chain handleVersioned(String type, MpegVersionedSectionHandler handler) {
      handlers.add(new Handler(type, false, handler));
      return this;
    }

    /**
     * @param type The FourCC of the section for which a handler is specified
     * @param finish Whether to stop reading after this section
     * @param handler The handler which expects versioned section info
     * @return this
     */
    public Chain handleVersioned(String type, boolean finish, MpegVersionedSectionHandler handler) {
      handlers.add(new Handler(type, finish, handler));
      return this;
    }

    /**
     * Assign a parsing stop checker to this chain.
     * @param stopChecker Stop checker.
     * @return this
     */
    public Chain stopChecker(MpegParseStopChecker stopChecker) {
      this.stopChecker = stopChecker;
      return this;
    }

    /**
     * Process the current section with all the handlers specified so far
     * @throws IOException On read error
     */
    public void run() throws IOException {
      MpegSectionInfo child;
      boolean finished = false;

      while (!finished && (child = reader.nextChild(parent)) != null) {
        finished = stopChecker != null && stopChecker.check(child, true);

        if (!finished) {
          processHandlers(child);

          finished = stopChecker != null && stopChecker.check(child, false);
        }

        reader.skip(child);
      }
    }

    private void processHandlers(MpegSectionInfo child) throws IOException {
      for (Handler handler : handlers) {
        if (handler.type.equals(child.type)) {
          handleSection(child, handler);
        }
      }
    }

    private boolean handleSection(MpegSectionInfo child, Handler handler) throws IOException {
      if (handler.sectionHandler instanceof MpegVersionedSectionHandler) {
        MpegVersionedSectionInfo versioned = parseFlagsForSection(reader.data, child);
        ((MpegVersionedSectionHandler) handler.sectionHandler).handle(versioned);
      } else {
        ((MpegSectionHandler) handler.sectionHandler).handle(child);
      }

      return !handler.terminator;
    }
  }

  private static class Handler {
    private final String type;
    private final boolean terminator;
    private final Object sectionHandler;

    private Handler(String type, boolean terminator, Object sectionHandler) {
      this.type = type;
      this.terminator = terminator;
      this.sectionHandler = sectionHandler;
    }
  }
}
