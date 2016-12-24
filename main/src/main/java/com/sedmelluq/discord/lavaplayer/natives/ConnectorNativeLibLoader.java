package com.sedmelluq.discord.lavaplayer.natives;

import static com.sedmelluq.discord.lavaplayer.natives.NativeLibLoader.WIN_X86;
import static com.sedmelluq.discord.lavaplayer.natives.NativeLibLoader.WIN_X86_64;

/**
 * Methods for loading the connector library.
 */
public class ConnectorNativeLibLoader {
  /**
   * Loads the connector library with its dependencies for the current system
   */
  public static void loadConnectorLibrary() {
    NativeLibLoader.load(ConnectorNativeLibLoader.class, WIN_X86_64, "libmpg123-0");
    NativeLibLoader.load(ConnectorNativeLibLoader.class, WIN_X86, "libgcc_s_sjlj-1");
    NativeLibLoader.load(ConnectorNativeLibLoader.class, WIN_X86, "libmpg123-0");
    NativeLibLoader.load(ConnectorNativeLibLoader.class, "connector");
  }
}
