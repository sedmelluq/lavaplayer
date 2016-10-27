package com.sedmelluq.discord.lavaplayer.container;

import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.io.SavedHeadSeekableInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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

  /**
   * @param reference Reference to the track with an identifier, used in the AudioTrackInfo in result
   * @param inputStream Input stream of the file
   * @return Result of detection
   */
  public static MediaContainerDetectionResult detectContainer(AudioReference reference, SeekableInputStream inputStream) {
    try {
      SavedHeadSeekableInputStream savedHeadInputStream = new SavedHeadSeekableInputStream(inputStream, HEAD_MARK_LIMIT);
      savedHeadInputStream.loadHead();

      for (MediaContainer container : MediaContainer.class.getEnumConstants()) {
        savedHeadInputStream.seek(0);
        MediaContainerDetectionResult result = checkContainer(container, reference, savedHeadInputStream);

        if (result != null) {
          return result;
        }
      }
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions("Could not read the file for detecting file type.", SUSPICIOUS, e);
    }

    return new MediaContainerDetectionResult();
  }

  private static MediaContainerDetectionResult checkContainer(MediaContainer container, AudioReference reference, SeekableInputStream inputStream) {
    try {
      return container.probe.probe(reference, inputStream);
    } catch (Exception e) {
      log.warn("Attempting to detect file with container {} failed.", container.name(), e);
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
}
