package com.sedmelluq.discord.lavaplayer.tools;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Helper methods related to strings and maps.
 */
public class DataFormatTools {
  private static final Pattern lineSplitPattern = Pattern.compile("[\\r\\n\\s]*\\n[\\r\\n\\s]*");

  /**
   * Extract text between the first subsequent occurrences of start and end in haystack
   * @param haystack The text to search from
   * @param start The text after which to start extracting
   * @param end The text before which to stop extracting
   * @return The extracted string
   */
  public static String extractBetween(String haystack, String start, String end) {
    int startMatch = haystack.indexOf(start);

    if (startMatch >= 0) {
      int startPosition = startMatch + start.length();
      int endPosition = haystack.indexOf(end, startPosition);

      if (endPosition >= 0) {
        return haystack.substring(startPosition, endPosition);
      }
    }

    return null;
  }

  /**
   * Converts name value pairs to a map, with the last entry for each name being present.
   * @param pairs Name value pairs to convert
   * @return The resulting map
   */
  public static Map<String, String> convertToMapLayout(Collection<NameValuePair> pairs) {
    Map<String, String> map = new HashMap<>();
    for (NameValuePair pair : pairs) {
      map.put(pair.getName(), pair.getValue());
    }
    return map;
  }

  /**
   * Returns the specified default value if the value itself is null.
   *
   * @param value Value to check
   * @param defaultValue Default value to return if value is null
   * @param <T> The type of the value
   * @return Value or default value
   */
  public static <T> T defaultOnNull(T value, T defaultValue) {
    return value == null ? defaultValue : value;
  }

  /**
   * Consumes a stream and returns it as lines.
   *
   * @param inputStream Input stream to consume.
   * @param charset Character set of the stream
   * @return Lines from the stream
   * @throws IOException On read error
   */
  public static String[] streamToLines(InputStream inputStream, Charset charset) throws IOException {
    String text = IOUtils.toString(inputStream, charset);
    return lineSplitPattern.split(text);
  }

  /**
   * Converts duration in the format HH:mm:ss (or mm:ss or ss) to milliseconds. Does not support day count.
   *
   * @param durationText Duration in text format.
   * @return Duration in milliseconds.
   */
  public static int durationTextToMillis(String durationText) {
    int length = 0;

    for (String part : durationText.split(":")) {
      length = length * 60 + Integer.valueOf(part);
    }

    return length;
  }
}
