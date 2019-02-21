package com.sedmelluq.lava.common.natives;

public class NativeProperties {
  private static final String PREFIX = "lava.native.";

  public static String get(String libraryName, String property, String defaultValue) {
    return System.getProperty(
        PREFIX + libraryName + "." + property,
        System.getProperty(PREFIX + property, defaultValue)
    );
  }
}
