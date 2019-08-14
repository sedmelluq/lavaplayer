package com.sedmelluq.discord.lavaplayer.remote.message;

/**
 * A message detailing track and performance statistics that a node includes in every response.
 */
public class NodeStatisticsMessage implements RemoteMessage {
  /**
   * The number of tracks that are not paused
   */
  public final int playingTrackCount;
  /**
   * Total number of tracks being processed by the node
   */
  public final int totalTrackCount;
  /**
   * Total CPU usage of the system
   */
  public final float systemCpuUsage;
  /**
   * CPU usage of the node process
   */
  public final float processCpuUsage;

  /**
   * @param playingTrackCount The number of tracks that are not paused
   * @param totalTrackCount Total number of tracks being processed by the node
   * @param systemCpuUsage Total CPU usage of the machine
   * @param processCpuUsage CPU usage of the node process
   */
  public NodeStatisticsMessage(int playingTrackCount, int totalTrackCount, float systemCpuUsage, float processCpuUsage) {
    this.playingTrackCount = playingTrackCount;
    this.totalTrackCount = totalTrackCount;
    this.systemCpuUsage = systemCpuUsage;
    this.processCpuUsage = processCpuUsage;
  }
}
