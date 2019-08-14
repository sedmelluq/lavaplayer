package com.sedmelluq.discord.lavaplayer.source.stream;

import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for extended M3U lines, handles the format where directives have named argumentsm, for example:
 * #SOMETHING:FOO="thing",BAR=4
 */
public class ExtendedM3uParser {
  private static final Pattern directiveArgumentPattern = Pattern.compile("([A-Z-]+)=(?:\"([^\"]*)\"|([^,]*))(?:,|\\z)");

  /**
   * Parses one line.
   * @param line Line.
   * @return Line object describing the directive or data on the line.
   */
  public static Line parseLine(String line) {
    String trimmed = line.trim();

    if (trimmed.isEmpty()) {
      return Line.EMPTY_LINE;
    } else if (!trimmed.startsWith("#")) {
      return new Line(trimmed, null, Collections.emptyMap(), null);
    } else {
      return parseDirectiveLine(trimmed);
    }
  }

  private static Line parseDirectiveLine(String line) {
    String[] parts = line.split(":", 2);

    if (parts.length == 1) {
      return new Line(null, line.substring(1), Collections.emptyMap(), "");
    }

    Matcher matcher = directiveArgumentPattern.matcher(parts[1]);
    Map<String, String> arguments = new HashMap<>();

    while (matcher.find()) {
      arguments.put(matcher.group(1), DataFormatTools.defaultOnNull(matcher.group(2), matcher.group(3)));
    }

    return new Line(null, parts[0].substring(1), arguments, parts[1]);
  }

  /**
   * Parsed extended M3U line info. May be either an empty line (isDirective() and isData() both false), a directive
   * or a data line.
   */
  public static class Line {
    private static final Line EMPTY_LINE = new Line(null, null, null, null);

    /**
     * The data of a data line.
     */
    public final String lineData;
    /**
     * Directive name of a directive line.
     */
    public final String directiveName;
    /**
     * Directive arguments of a directive line.
     */
    public final Map<String, String> directiveArguments;
    /**
     * Raw unprocessed directive extra data (where arguments are parsed from).
     */
    public final String extraData;

    private Line(String lineData, String directiveName, Map<String, String> directiveArguments, String extraData) {
      this.lineData = lineData;
      this.directiveName = directiveName;
      this.directiveArguments = directiveArguments;
      this.extraData = extraData;
    }

    /**
     * @return True if it is a directive line.
     */
    public boolean isDirective() {
      return directiveName != null;
    }

    /**
     * @return True if it is a data line.
     */
    public boolean isData() {
      return lineData != null;
    }
  }
}
