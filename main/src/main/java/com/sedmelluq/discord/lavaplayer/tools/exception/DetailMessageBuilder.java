package com.sedmelluq.discord.lavaplayer.tools.exception;

import java.util.function.IntPredicate;

public class DetailMessageBuilder {
  public final StringBuilder builder;

  public DetailMessageBuilder() {
    this.builder = new StringBuilder();
  }

  public void appendHeader(String header) {
    builder.append(header);
  }

  public void appendField(String name, Object value) {
    builder.append("\n  ").append(name).append(": ");

    if (value == null) {
      builder.append("<unspecified>");
    } else {
      builder.append(value.toString());
    }
  }

  public void appendField(String name, int value) {
    appendField(name, String.valueOf(value));
  }

  public <T> void appendArray(String label, boolean alwaysPrint, T[] array, IntPredicate check) {
    boolean started = false;

    for (int i = 0; i < array.length; i++) {
      if (check.test(i)) {
        if (!started) {
          builder.append("\n  ").append(label).append(": ");
          started = true;
        }

        builder.append(array[i]).append(", ");
      }
    }

    if (started) {
      builder.setLength(builder.length() - 2);
    } else if (alwaysPrint) {
      appendField(label, "NONE");
    }
  }

  @Override
  public String toString() {
    return builder.toString();
  }
}
