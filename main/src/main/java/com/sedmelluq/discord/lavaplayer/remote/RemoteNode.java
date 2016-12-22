package com.sedmelluq.discord.lavaplayer.remote;

import com.sedmelluq.discord.lavaplayer.remote.message.NodeStatisticsMessage;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;
import java.util.Map;

/**
 * A remote node interface which provides information about a specific node.
 */
public interface RemoteNode {
  /**
   * @return The address of this remote node.
   */
  String getAddress();

  /**
   * @return Gets the current state of connection between the node processor and the node.
   */
  ConnectionState getConnectionState();

  /**
   * @return The last statistics received from this node. May be null.
   */
  NodeStatisticsMessage getLastStatistics();

  /**
   * @return The minimum amount of time in milliseconds between the start time of two ticks.
   */
  int getTickMinimumInterval();

  /**
   * To record all ticks, it is possible to calculate the minimum amount of time which guarantees that none are
   * discarded in the internal history (minimumInterval * tickHistoryCapacity).
   *
   * @return The number of ticks that are kept in history.
   */
  int getTickHistoryCapacity();

  /**
   * @param reset Whether to reset the history so next calls will only contain new ones.
   * @return All the ticks in the history, up to the history capacity. In case of an overflow, newer ones will replace
   *         older ones.
   */
  List<Tick> getLastTicks(boolean reset);

  /**
   * @return The number of tracks being played by this player manager through this node.
   */
  int getPlayingTrackCount();

  /**
   * @return List of tracks being played by this node for the current player manager.
   */
  List<AudioTrack> getPlayingTracks();

  /**
   * @return Map containing the balancer penalty factors, with "Total" being the sum of all others.
   */
  Map<String, Integer> getBalancerPenaltyDetails();

  /**
   * Checks if a audio track is being played by this node.
   *
   * @param track The audio track.
   * @return True if this node is playing that track.
   */
  boolean isPlayingTrack(AudioTrack track);

  /**
   * Information about one request made to the node.
   */
  class Tick {
    /**
     * The time when the node processor started building the request to send to the node.
     */
    public final long startTime;
    /**
     * The time when the processing the response data from the node was finished.
     */
    public final long endTime;
    /**
     * Response code from the node. -1 in case of connection failure.
     */
    public final int responseCode;
    /**
     * The size of the request in bytes.
     */
    public final int requestSize;
    /**
     * The size of the uncompressed response in bytes.
     */
    public final int responseSize;

    /**
     * @param startTime The time when the node processor started building the request to send to the node.
     * @param endTime The time when the processing the response data from the node was finished.
     * @param responseCode Response code from the node. -1 in case of connection failure.
     * @param requestSize The size of the request in bytes.
     * @param responseSize The size of the uncompressed response in bytes.
     */
    public Tick(long startTime, long endTime, int responseCode, int requestSize, int responseSize) {
      this.startTime = startTime;
      this.endTime = endTime;
      this.responseCode = responseCode;
      this.requestSize = requestSize;
      this.responseSize = responseSize;
    }
  }

  /**
   * State of the connection to this node.
   */
  enum ConnectionState {
    /**
     * The node processor is current in the middle of attempting a connection to this node. Happens on every
     * reconnection attempt, even if the node has already been offline for a period of time.
     */
    PENDING,
    /**
     * The node is currently online, new tracks can be sent to this node.
     */
    ONLINE,
    /**
     * The node is offline, this is the state the node has before attempting the first request and after any failed
     * request to a node until a new request is successful.
     */
    OFFLINE,
    /**
     * This node has been removed from the list of nodes to use. In case a node with the same address is added, this
     * instance will not be reactivated, but a new one should be retrieved.
     */
    REMOVED;

    /**
     * @return Shortcut for ordinal.
     */
    public int id() {
      return ordinal();
    }
  }
}
