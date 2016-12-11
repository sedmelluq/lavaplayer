package com.sedmelluq.discord.lavaplayer.tools;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory for daemon threads.
 */
public class DaemonThreadFactory implements ThreadFactory {
  private static final AtomicInteger poolNumber = new AtomicInteger(1);
  private final ThreadGroup group;
  private final AtomicInteger threadNumber = new AtomicInteger(1);
  private final String namePrefix;

  /**
   * @param name Name that will be included in thread names.
   */
  public DaemonThreadFactory(String name) {
    SecurityManager securityManager = System.getSecurityManager();

    group = (securityManager != null) ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
    namePrefix = "lava-daemon-pool-" + name + "-" + poolNumber.getAndIncrement() + "-thread-";
  }

  @Override
  public Thread newThread(Runnable runnable) {
    Thread thread = new Thread(group, runnable, namePrefix + threadNumber.getAndIncrement(), 0);
    thread.setDaemon(true);
    thread.setPriority(Thread.NORM_PRIORITY);
    return thread;
  }
}
