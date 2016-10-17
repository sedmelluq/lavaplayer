package com.sedmelluq.discord.lavaplayer.jdaudp;

import com.sedmelluq.discord.lavaplayer.player.hook.AudioOutputHook;
import com.sedmelluq.discord.lavaplayer.player.hook.AudioOutputHookFactory;
import com.sedmelluq.discord.lavaplayer.udpqueue.natives.UdpQueueManager;

import java.util.concurrent.TimeUnit;

/**
 * Manager that handles the hooks that redirect audio frames to native UDP packet scheduling library for JDA.
 */
public class JdaUdpQueueingHookManager implements AudioOutputHookFactory {
  private static final int BUFFER_DURATION = 400;
  private static final int PACKET_INTERVAL = 20;
  private static final int MAXIMUM_PACKET_SIZE = 4096;

  private final UdpQueueManager udpQueueManager = new UdpQueueManager(BUFFER_DURATION / PACKET_INTERVAL,
      TimeUnit.MILLISECONDS.toNanos(PACKET_INTERVAL), MAXIMUM_PACKET_SIZE);

  /**
   * Start the UDP packet scheduler thread.
   */
  public void startDispatcher() {
    Thread thread = new Thread(udpQueueManager::process);
    thread.setPriority((Thread.NORM_PRIORITY + Thread.MAX_PRIORITY) / 2);
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Shut down the hook manager and free its native resources and threads.
   */
  public void shutdown() {
    udpQueueManager.close();
  }

  @Override
  public AudioOutputHook createOutputHook() {
    return new JdaUdpQueueingHook(udpQueueManager);
  }
}
