package com.sedmelluq.discord.lavaplayer.demo.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class TrackBoxBuilder {
  private static final String TOP_LEFT_CORNER = "\u2554";
  private static final String TOP_RIGHT_CORNER = "\u2557";
  private static final String BOTTOM_LEFT_CORNER = "\u255a";
  private static final String BOTTOM_RIGHT_CORNER = "\u255d";
  private static final String BORDER_HORIZONTAL = "\u2550";
  private static final String BORDER_VERTICAL = "\u2551";
  private static final String PROGRESS_FILL = "\u25a0";
  private static final String PROGRESS_EMPTY = "\u2015";

  public static String buildTrackBox(int width, AudioTrack track, boolean isPaused, int volume) {
    return boxify(width, buildFirstLine(width - 4, track), buildSecondLine(width - 4, track, isPaused, volume));
  }

  private static String buildFirstLine(int width, AudioTrack track) {
    StringBuilder builder = new StringBuilder();
    String title = track.getInfo().title;
    int titleWidth = width - 7;

    if (title.length() > titleWidth) {
      builder.append(title.substring(0, titleWidth - 3));
      builder.append("...");
    } else {
      builder.append(title);
    }

    return builder.toString();
  }

  private static String buildSecondLine(int width, AudioTrack track, boolean isPaused, int volume) {
    String cornerText = isPaused ? "PAUSED" : volume + "%";

    String duration = formatTiming(track.getDuration(), track.getDuration());
    String position = formatTiming(track.getPosition(), track.getDuration());
    int spacing = duration.length() - position.length();
    int barLength = width - duration.length() - position.length() - spacing - 14;

    float progress = (float) Math.min(track.getPosition(), track.getDuration()) / (float) Math.max(track.getDuration(), 1);
    int progressBlocks = Math.round(progress * barLength);

    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < 6 - cornerText.length(); i++) {
      builder.append(" ");
    }

    builder.append(cornerText);

    builder.append(" [");
    for (int i = 0; i < barLength; i++) {
      builder.append(i < progressBlocks ? PROGRESS_FILL : PROGRESS_EMPTY);
    }
    builder.append("]");

    for (int i = 0; i < spacing + 1; i++) {
      builder.append(" ");
    }

    builder.append(position);
    builder.append(" of ");
    builder.append(duration);

    builder.append(" ");
    builder.append(TOP_RIGHT_CORNER);

    return builder.toString();
  }

  private static String formatTiming(long timing, long maximum) {
    timing = Math.min(timing, maximum) / 1000;

    long seconds = timing % 60;
    timing /= 60;
    long minutes = timing % 60;
    timing /= 60;
    long hours = timing;

    if (maximum >= 3600000L) {
      return String.format("%d:%02d:%02d", hours, minutes, seconds);
    } else {
      return String.format("%d:%02d", minutes, seconds);
    }
  }

  private static void boxifyLine(StringBuilder builder, String line) {
    builder.append(BORDER_VERTICAL);
    builder.append(" ");
    builder.append(line);
    builder.append("\n");
  }

  private static String boxify(int width, String firstLine, String secondLine) {
    StringBuilder builder = new StringBuilder();

    builder.append("```");
    builder.append(TOP_LEFT_CORNER);
    for (int i = 0; i < width - 1; i++) {
      builder.append(BORDER_HORIZONTAL);
    }
    builder.append("\n");

    boxifyLine(builder, firstLine);
    boxifyLine(builder, secondLine);

    builder.append(BOTTOM_LEFT_CORNER);
    for (int i = 0; i < width - 2; i++) {
      builder.append(BORDER_HORIZONTAL);
    }
    builder.append(BOTTOM_RIGHT_CORNER);
    builder.append("```");

    return builder.toString();
  }
}
