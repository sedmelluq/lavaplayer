package com.sedmelluq.discord.lavaplayer.tools.exception;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;

public class EnvironmentInformation extends Exception {
  private static final String[] PROPERTIES = new String[] {
      "os.arch",
      "os.name",
      "os.version",
      "java.vendor",
      "java.version",
      "java.runtime.version",
      "java.vm.version"
  };

  public static final EnvironmentInformation INSTANCE = create();

  private EnvironmentInformation(String message) {
    super(message, null, false, false);
  }

  private static EnvironmentInformation create() {
    DetailMessageBuilder builder = new DetailMessageBuilder();
    builder.appendField("lavaplayer.version", PlayerLibrary.VERSION);

    for (String property : PROPERTIES) {
      builder.appendField(property, System.getProperty(property));
    }

    return new EnvironmentInformation(builder.toString());
  }
}
