package com.sedmelluq.discord.lavaplayer.container;

import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.io.GreedyInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.SavedHeadSeekableInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult.unknownFormat;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Detects the container used by a file and whether the specific file is supported for playback.
 */
public class MediaContainerDetection {
  public static final String UNKNOWN_TITLE = "Unknown title";
  public static final String UNKNOWN_ARTIST = "Unknown artist";
  public static final int STREAM_SCAN_DISTANCE = 1000;

  private static final Logger log = LoggerFactory.getLogger(MediaContainerDetection.class);

  private static final int HEAD_MARK_LIMIT = 1024;

  private final MediaContainerRegistry containerRegistry;
  private final AudioReference reference;
  private final SeekableInputStream inputStream;
  private final MediaContainerHints hints;

  /**
   * @param reference Reference to the track with an identifier, used in the AudioTrackInfo in result
   * @param inputStream Input stream of the file
   * @param hints Hints about the format (mime type, extension)
   */
  public MediaContainerDetection(MediaContainerRegistry containerRegistry, AudioReference reference,
                                 SeekableInputStream inputStream, MediaContainerHints hints) {

    this.containerRegistry = containerRegistry;
    this.reference = reference;
    this.inputStream = inputStream;
    this.hints = hints;
  }

  /**
   * @return Result of detection.
   */
  public MediaContainerDetectionResult detectContainer() {
    MediaContainerDetectionResult result;

    try {
      SavedHeadSeekableInputStream savedHeadInputStream = new SavedHeadSeekableInputStream(inputStream, HEAD_MARK_LIMIT);
      savedHeadInputStream.loadHead();

      result = detectContainer(savedHeadInputStream, true);

      if (result == null) {
        result = detectContainer(savedHeadInputStream, false);
      }
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions("Could not read the file for detecting file type.", SUSPICIOUS, e);
    }

    return result != null ? result : unknownFormat();
  }

  private MediaContainerDetectionResult detectContainer(SeekableInputStream innerStream, boolean matchHints)
      throws IOException {

    for (MediaContainerProbe probe : containerRegistry.getAll()) {
      if (matchHints == probe.matchesHints(hints)) {
        innerStream.seek(0);
        MediaContainerDetectionResult result = checkContainer(probe, reference, innerStream);

        if (result != null) {
          return result;
        }
      }
    }

    return null;
  }

  private static MediaContainerDetectionResult checkContainer(MediaContainerProbe probe, AudioReference reference,
                                                              SeekableInputStream inputStream) {

    try {
      return probe.probe(reference, inputStream);
    } catch (Exception e) {
      log.warn("Attempting to detect file with container {} failed.", probe.getName(), e);
      return null;
    }
  }

  /**
   * Checks the next bytes in the stream if they match the specified bytes. The input may contain -1 as byte value as
   * a wildcard, which means the value of this byte does not matter. The position of the stream is restored on return.
   *
   * @param stream Input stream to read the bytes from
   * @param match Bytes that the next bytes from input stream should match (-1 as wildcard
   * @return True if the bytes matched
   * @throws IOException On IO error
   */
  public static boolean checkNextBytes(SeekableInputStream stream, int[] match) throws IOException {
    return checkNextBytes(stream, match, true);
  }

  /**
   * Checks the next bytes in the stream if they match the specified bytes. The input may contain -1 as byte value as
   * a wildcard, which means the value of this byte does not matter.
   *
   * @param stream Input stream to read the bytes from
   * @param match Bytes that the next bytes from input stream should match (-1 as wildcard
   * @param rewind If set to true, restores the original position of the stream after checking
   * @return True if the bytes matched
   * @throws IOException On IO error
   */
  public static boolean checkNextBytes(SeekableInputStream stream, int[] match, boolean rewind) throws IOException {
    long position = stream.getPosition();
    boolean result = true;

    for (int matchByte : match) {
      int inputByte = stream.read();

      if (inputByte == -1 || (matchByte != -1 && matchByte != inputByte)) {
        result = false;
        break;
      }
    }

    if (rewind) {
      stream.seek(position);
    }

    return result;
  }

  /**
   * Check if the next bytes in the stream match the specified regex pattern.
   *
   * @param stream Input stream to read the bytes from
   * @param distance Maximum number of bytes to read for matching
   * @param pattern Pattern to match against
   * @param charset Charset to use to decode the bytes
   * @return True if the next bytes in the stream are a match
   * @throws IOException On read error
   */
  public static boolean matchNextBytesAsRegex(SeekableInputStream stream, int distance, Pattern pattern, Charset charset) throws IOException {
    long position = stream.getPosition();
    byte[] bytes = new byte[distance];

    int read = new GreedyInputStream(stream).read(bytes);
    stream.seek(position);

    if (read == -1) {
      return false;
    }

    String text = new String(bytes, 0, read, charset);
    return pattern.matcher(text).find();
  }
}
