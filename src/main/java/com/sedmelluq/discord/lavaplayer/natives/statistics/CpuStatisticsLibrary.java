package com.sedmelluq.discord.lavaplayer.natives.statistics;

import com.sedmelluq.discord.lavaplayer.natives.ConnectorNativeLibLoader;

class CpuStatisticsLibrary {
  private CpuStatisticsLibrary() {

  }

  static CpuStatisticsLibrary getInstance() {
    ConnectorNativeLibLoader.loadConnectorLibrary();
    return new CpuStatisticsLibrary();
  }

  native void getSystemTimes(long[] timingArray);

  enum Timings {
    SYSTEM_TOTAL,
    SYSTEM_USER,
    SYSTEM_KERNEL,
    PROCESS_USER,
    PROCESS_KERNEL;

    int id() {
      return ordinal();
    }
  }
}
