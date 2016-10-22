package com.sedmelluq.discord.lavaplayer.container;

import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.io.SavedHeadSeekableInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
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

  private static final Logger log = LoggerFactory.getLogger(MediaContainerDetection.class);

  private static final int HEAD_MARK_LIMIT = 512;

  /**
   * @param identifier Identifier of the track, used in the AudioTrackInfo in result
   * @param inputStream Input stream of the file
   * @return Result of detection
   */
  public static MediaContainerDetection.Result detectContainer(String identifier, SeekableInputStream inputStream) {
    try {
      SavedHeadSeekableInputStream savedHeadInputStream = new SavedHeadSeekableInputStream(inputStream, HEAD_MARK_LIMIT);
      savedHeadInputStream.loadHead();

      for (MediaContainer container : MediaContainer.class.getEnumConstants()) {
        savedHeadInputStream.seek(0);
        MediaContainerDetection.Result result = checkContainer(container, identifier, savedHeadInputStream);

        if (result != null) {
          return result;
        }
      }
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions("Could not read the file for detecting file type.", SUSPICIOUS, e);
    }

    return new Result();
  }

  private static MediaContainerDetection.Result checkContainer(MediaContainer container, String identifier, SeekableInputStream inputStream) {
    try {
      return container.probe.probe(identifier, inputStream);
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

  /**
   * Result of audio container detection.
   */
  public static class Result {
    private final AudioTrackInfo trackInfo;
    private final MediaContainerProbe containerProbe;
    private final String unsupportedReason;

    /**
     * Constructor for supported file.
     *
     * @param containerProbe Probe of the container
     * @param trackInfo Track info for the file
     */
    public Result(MediaContainerProbe containerProbe, AudioTrackInfo trackInfo) {
      this.trackInfo = trackInfo;
      this.containerProbe = containerProbe;
      this.unsupportedReason = null;
    }

    /**
     * Constructor for unsupported file of a known container.
     *
     * @param containerProbe Probe of the container
     * @param unsupportedReason The reason why this track is not supported
     */
    public Result(MediaContainerProbe containerProbe, String unsupportedReason) {
      this.trackInfo = null;
      this.containerProbe = containerProbe;
      this.unsupportedReason = unsupportedReason;
    }

    /**
     * Constructor for unknown format result.
     */
    public Result() {
      trackInfo = null;
      containerProbe = null;
      unsupportedReason = null;
    }

    /**
     * @return If the container this file uses was detected. In case this returns true, the container probe is non-null.
     */
    public boolean isContainerDetected() {
      return containerProbe != null;
    }

    /**
     * @return The probe for the container of the file
     */
    public MediaContainerProbe getContainerProbe() {
      return containerProbe;
    }

    /**
     * @return Whether this specific file is supported. If this returns true, the track info is non-null. Otherwise
     *         the reason why this file is not supported can be retrieved via getUnsupportedReason().
     */
    public boolean isSupportedFile() {
      return isContainerDetected() && unsupportedReason == null;
    }

    /**
     * @return The reason why this track is not supported.
     */
    public String getUnsupportedReason() {
      return unsupportedReason;
    }

    /**
     * @return Track info for the detected file.
     */
    public AudioTrackInfo getTrackInfo() {
      return trackInfo;
    }
  }
}
