package com.sedmelluq.discord.lavaplayer.node;

import com.sedmelluq.discord.lavaplayer.natives.statistics.CpuStatistics;
import com.sedmelluq.discord.lavaplayer.remote.message.NodeStatisticsMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;

@Component
public class StatisticsManager {
  private static final int SAMPLE_COUNT = 5;

  private static final CpuStatistics cpuStatistics = new CpuStatistics();

  private final Object synchronizer;
  private final ArrayDeque<CpuStatistics.Times> runningCpuStatistics;

  private float systemCpuUsage;
  private float processCpuUsage;
  private int playingTrackCount;
  private int totalTrackCount;

  public StatisticsManager() {
    synchronizer = new Object();
    runningCpuStatistics = new ArrayDeque<>();
  }

  public void updateTrackStatistics(int playingTrackCount, int totalTrackCount) {
    synchronized (synchronizer) {
      this.playingTrackCount = playingTrackCount;
      this.totalTrackCount = totalTrackCount;
    }
  }

  public void increaseTrackCount() {
    synchronized (synchronizer) {
      this.playingTrackCount++;
      this.totalTrackCount++;
    }
  }

  public NodeStatisticsMessage getStatistics() {
    synchronized (synchronizer) {
      return new NodeStatisticsMessage(playingTrackCount, totalTrackCount, systemCpuUsage, processCpuUsage);
    }
  }

  @Scheduled(fixedRate = 1000)
  private void pollCpuStatistics() {
    CpuStatistics.Times current = cpuStatistics.getSystemTimes();

    synchronized (synchronizer) {
      if (runningCpuStatistics.size() >= SAMPLE_COUNT) {
        runningCpuStatistics.removeFirst();
      }

      runningCpuStatistics.add(current);

      CpuStatistics.Times difference = CpuStatistics.diff(runningCpuStatistics.getFirst(), current);
      systemCpuUsage = difference.getSystemUsage();
      processCpuUsage = difference.getProcessUsage();
    }
  }
}
