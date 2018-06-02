package com.sedmelluq.discord.lavaplayer.remote;

import com.sedmelluq.discord.lavaplayer.remote.message.NodeStatisticsMessage;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;

/**
 * Takes over tracks of offline remote nodes.
 */
public class AbandonedTrackManager {
  private static final Logger log = LoggerFactory.getLogger(AbandonedTrackManager.class);

  private static final long EXPIRE_THRESHOLD = TimeUnit.SECONDS.toMillis(10);
  private static final long CRITICAL_PENALTY = 750;

  private final BlockingQueue<AbandonedExecutor> abandonedExecutors;

  /**
   * Create an instance.
   */
  public AbandonedTrackManager() {
    this.abandonedExecutors = new ArrayBlockingQueue<>(2000);
  }

  /**
   * Adds a track executor to abandoned tracks. The abandoned track manager will take over managing its lifecycle and
   * the caller should not use it any further.
   *
   * @param executor The executor to register as an abandoned track.
   */
  public void add(RemoteAudioTrackExecutor executor) {
    if (abandonedExecutors.offer(new AbandonedExecutor(System.currentTimeMillis(), executor))) {
      log.debug("{} has been put up for adoption.", executor);
    } else {
      log.debug("{} has been discarded, adoption queue is full.", executor);

      executor.dispatchException(new FriendlyException("Cannot find a node to play the track on.", COMMON, null));
      executor.stop();
    }
  }

  /**
   * Distributes any abandoned tracks between the specified nodes. Only online nodes which are not under too heavy load
   * are used. The number of tracks that can be assigned to a node depends on the number of tracks it is already
   * processing (track count can increase only by 1/15th on each call, or by 5).
   *
   * @param nodes Remote nodes to give abandoned tracks to.
   */
  public void distribute(List<RemoteNodeProcessor> nodes) {
    if (abandonedExecutors.isEmpty()) {
      return;
    }

    List<Adopter> adopters = findAdopters(nodes);
    AbandonedExecutor executor;
    long currentTime = System.currentTimeMillis();
    int maximum = getMaximumAdoptions(adopters);
    int assigned = 0;

    while (assigned < maximum && (executor = abandonedExecutors.poll()) != null) {
      if (checkValidity(executor, currentTime)) {
        Adopter adopter = selectNextAdopter(adopters);

        if (adopter != null) {
          log.debug("Node {} is adopting {}.", adopter.node.getAddress(), executor.executor);

          adopter.node.startPlaying(executor.executor);
          assigned++;
        } else {
          log.debug("No node available for adopting {}", executor.executor);
        }
      }
    }
  }

  /**
   * Shut down the abandoned track manager, stopping any tracks.
   */
  public void shutdown() {
    AbandonedExecutor executor;

    while ((executor = abandonedExecutors.poll()) != null) {
      executor.executor.dispatchException(new FriendlyException("Node system was shut down.", COMMON, null));
      executor.executor.stop();
    }
  }

  /**
   * Remove expired or stopped tracks from the abandoned track queue.
   */
  public void drainExpired() {
    AbandonedExecutor executor;
    long currentTime = System.currentTimeMillis();

    while ((executor = abandonedExecutors.peek()) != null) {
      if (!checkValidity(executor, currentTime) && abandonedExecutors.remove(executor)) {
        log.debug("Abandoned executor {} removed from queue.", executor.executor);
      }
    }
  }

  private boolean checkValidity(AbandonedExecutor executor, long currentTime) {
    long expirationTime = currentTime - EXPIRE_THRESHOLD;

    if (executor.executor.getState() == AudioTrackState.FINISHED) {
      log.debug("{} has been cleared from adoption queue because it was stopped.", executor.executor);
      return false;
    } else if (executor.orphanedTime < expirationTime) {
      log.debug("{} has been cleared from adoption queue because it expired.", executor.executor);

      executor.executor.dispatchException(new FriendlyException("Could not find next node to play on.", COMMON, null));
      executor.executor.stop();
      return false;
    } else {
      return true;
    }
  }

  private List<Adopter> findAdopters(List<RemoteNodeProcessor> nodes) {
    List<Adopter> adopters = new ArrayList<>();

    for (RemoteNodeProcessor node : nodes) {
      int penalty = node.getBalancerPenalty();
      NodeStatisticsMessage statistics = node.getLastStatistics();

      if (penalty >= CRITICAL_PENALTY || statistics == null) {
        continue;
      }

      int maximumAdoptions = Math.max(5, statistics.playingTrackCount / 15);
      adopters.add(new Adopter(node, maximumAdoptions));
    }

    return adopters;
  }

  private Adopter selectNextAdopter(List<Adopter> adopters) {
    Adopter selected = null;

    for (Adopter adopter : adopters) {
      if (adopter.adoptions < adopter.maximumAdoptions && (selected == null || adopter.fillRate() > selected.fillRate())) {
        selected = adopter;
      }
    }

    if (selected != null) {
      selected.adoptions++;
    }

    return selected;
  }

  private int getMaximumAdoptions(List<Adopter> adopters) {
    int total = 0;

    for (Adopter adopter : adopters) {
      total += adopter.maximumAdoptions;
    }

    return total;
  }

  private static class AbandonedExecutor {
    private final long orphanedTime;
    private final RemoteAudioTrackExecutor executor;

    private AbandonedExecutor(long orphanedTime, RemoteAudioTrackExecutor executor) {
      this.orphanedTime = orphanedTime;
      this.executor = executor;
    }
  }

  private static class Adopter {
    private final RemoteNodeProcessor node;
    private final long maximumAdoptions;
    private int adoptions;

    private Adopter(RemoteNodeProcessor node, long maximumAdoptions) {
      this.node = node;
      this.maximumAdoptions = maximumAdoptions;
      this.adoptions = 0;
    }

    private float fillRate() {
      return (float) adoptions / maximumAdoptions;
    }
  }
}
