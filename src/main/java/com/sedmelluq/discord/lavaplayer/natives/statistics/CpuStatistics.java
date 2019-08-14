package com.sedmelluq.discord.lavaplayer.natives.statistics;

import static com.sedmelluq.discord.lavaplayer.natives.statistics.CpuStatisticsLibrary.Timings.PROCESS_KERNEL;
import static com.sedmelluq.discord.lavaplayer.natives.statistics.CpuStatisticsLibrary.Timings.PROCESS_USER;
import static com.sedmelluq.discord.lavaplayer.natives.statistics.CpuStatisticsLibrary.Timings.SYSTEM_KERNEL;
import static com.sedmelluq.discord.lavaplayer.natives.statistics.CpuStatisticsLibrary.Timings.SYSTEM_TOTAL;
import static com.sedmelluq.discord.lavaplayer.natives.statistics.CpuStatisticsLibrary.Timings.SYSTEM_USER;

/**
 * Provides information about system CPU usage.
 */
public class CpuStatistics {
  private static final int TIMINGS_LENGTH = CpuStatisticsLibrary.Timings.class.getEnumConstants().length;

  private final CpuStatisticsLibrary library = CpuStatisticsLibrary.getInstance();

  /**
   * @return Absolute CPU timings at the current moment
   */
  public Times getSystemTimes() {
    long[] values = new long[TIMINGS_LENGTH];
    library.getSystemTimes(values);

    return new Times(
        values[SYSTEM_TOTAL.id()],
        values[SYSTEM_USER.id()],
        values[SYSTEM_KERNEL.id()],
        values[PROCESS_USER.id()],
        values[PROCESS_KERNEL.id()]
    );
  }

  /**
   * CPU timings
   */
  public static class Times {
    /**
     * Total amount of CPU time since system start
     */
    public final long systemTotal;
    /**
     * Total amount of CPU time spent in user mode
     */
    public final long systemUser;
    /**
     * Total amount of CPU time spent in kernel mode
     */
    public final long systemKernel;
    /**
     * Total amount of CPU time this process has spent in user mode
     */
    public final long processUser;
    /**
     * Total amount of CPU time this process has spent in kernel mode
     */
    public final long processKernel;

    /**
     * @param systemTotal Total amount of CPU time since system start
     * @param systemUser Total amount of CPU time spent in user mode
     * @param systemKernel Total amount of CPU time spent in kernel mode
     * @param processUser Total amount of CPU time this process has spent in user mode
     * @param processKernel Total amount of CPU time this process has spent in kernel mode
     */
    public Times(long systemTotal, long systemUser, long systemKernel, long processUser, long processKernel) {
      this.systemTotal = systemTotal;
      this.systemUser = systemUser;
      this.systemKernel = systemKernel;
      this.processUser = processUser;
      this.processKernel = processKernel;
    }

    /**
     * @return The ratio of used CPU time to total CPU time
     */
    public float getSystemUsage() {
      if (systemTotal == 0) {
        return 0.0f;
      } else {
        return (float) (systemUser + systemKernel) / systemTotal;
      }
    }

    /**
     * @return The ratio of used CPU time by current process to total CPU time
     */
    public float getProcessUsage() {
      if (systemTotal == 0) {
        return 0.0f;
      } else {
        return (float) (processUser + processKernel) / systemTotal;
      }
    }
  }

  /**
   * @param old Older timing values
   * @param current Newer timing values
   * @return Difference between two timings
   */
  public static Times diff(Times old, Times current) {
    return new Times(
        current.systemTotal - old.systemTotal,
        current.systemUser - old.systemUser,
        current.systemKernel - old.systemKernel,
        current.processUser - old.processUser,
        current.processKernel - old.processKernel
    );
  }
}
