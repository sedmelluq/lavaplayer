package com.sedmelluq.discord.lavaplayer.remote;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;

/**
 * Registry of currently used remote nodes by a player manager.
 */
public interface RemoteNodeRegistry {
  /**
   * @return True if using remote nodes for audio processing is enabled.
   */
  boolean isEnabled();

  /**
   * Finds the node which is playing the specified track.
   *
   * @param track The track to check.
   * @return The node which is playing this track, or null if no node is playing it.
   */
  RemoteNode getNodeUsedForTrack(AudioTrack track);

  /**
   * @return List of all nodes currently in use (including ones which are offline).
   */
  List<RemoteNode> getNodes();
}
