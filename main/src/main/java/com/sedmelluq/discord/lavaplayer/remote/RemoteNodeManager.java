package com.sedmelluq.discord.lavaplayer.remote;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.DaemonThreadFactory;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioTrackExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Manager of remote nodes for audio processing.
 */
public class RemoteNodeManager extends AudioEventAdapter implements Runnable {
  private final AudioPlayerManager playerManager;
  private final List<RemoteNodeProcessor> processors;
  private final AtomicBoolean enabled;

  /**
   * @param playerManager Audio player manager
   */
  public RemoteNodeManager(AudioPlayerManager playerManager) {
    this.playerManager = playerManager;
    this.processors = new ArrayList<>();
    this.enabled = new AtomicBoolean();
  }

  /**
   * Enable and initialise the remote nodes.
   * @param nodeAddresses Addresses of remote nodes
   */
  public void initialise(List<String> nodeAddresses) {
    if (!enabled.compareAndSet(false, true)) {
      throw new IllegalStateException("Remote nodes already configured.");
    }

    ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(nodeAddresses.size() + 1, new DaemonThreadFactory("remote"));
    scheduledExecutor.scheduleAtFixedRate(this, 2000, 2000, TimeUnit.MILLISECONDS);

    for (String nodeAddress : nodeAddresses) {
      RemoteNodeProcessor processor = new RemoteNodeProcessor(playerManager, nodeAddress, scheduledExecutor);
      scheduledExecutor.submit(processor);
      processors.add(processor);
    }
  }

  /**
   * @return True if using remote nodes for audio processing is enabled
   */
  public boolean isEnabled() {
    return enabled.get();
  }

  /**
   * Start playing an audio track remotely.
   * @param remoteExecutor The executor of the track
   */
  public void startPlaying(RemoteAudioTrackExecutor remoteExecutor) {
    RemoteNodeProcessor processor = getNodeForNextTrack();

    processor.startPlaying(remoteExecutor);
  }

  private RemoteNodeProcessor getNodeForNextTrack() {
    int lowestPenalty = Integer.MAX_VALUE;
    RemoteNodeProcessor node = null;

    for (RemoteNodeProcessor processor : processors) {
      int penalty = processor.getBalancerPenalty();

      if (penalty < lowestPenalty) {
        lowestPenalty = penalty;
        node = processor;
      }
    }

    if (node == null) {
      throw new FriendlyException("No available machines for playing track.", SUSPICIOUS, null);
    }

    return node;
  }

  @Override
  public void onTrackEnd(AudioPlayer player, AudioTrack track, boolean interrupted) {
    AudioTrackExecutor executor = ((InternalAudioTrack) track).getActiveExecutor();

    if (interrupted && executor instanceof RemoteAudioTrackExecutor) {
      for (RemoteNodeProcessor processor : processors) {
        processor.trackEnded((RemoteAudioTrackExecutor) executor, true);
      }
    }
  }

  @Override
  public void run() {
    for (RemoteNodeProcessor processor : processors) {
      processor.processHealthCheck();
    }
  }
}
