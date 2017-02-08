package com.sedmelluq.discord.lavaplayer.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory for daemon threads.
 */
public class DaemonThreadFactory implements ThreadFactory {
  private static final Logger log = LoggerFactory.getLogger(DaemonThreadFactory.class);

  private static final AtomicInteger poolNumber = new AtomicInteger(1);
  private final ThreadGroup group;
  private final AtomicInteger threadNumber = new AtomicInteger(1);
  private final String namePrefix;
  private final Runnable exitCallback;

  /**
   * @param name Name that will be included in thread names.
   */
  public DaemonThreadFactory(String name) {
    this(name, null);
  }

  /**
   * @param name Name that will be included in thread names.
   * @param exitCallback Runnable to be executed when the thread exits.
   */
  public DaemonThreadFactory(String name, Runnable exitCallback) {
    SecurityManager securityManager = System.getSecurityManager();

    group = (securityManager != null) ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
    namePrefix = "lava-daemon-pool-" + name + "-" + poolNumber.getAndIncrement() + "-thread-";
    this.exitCallback = exitCallback;
  }

  @Override
  public Thread newThread(Runnable runnable) {
    Thread thread = new Thread(group, getThreadRunnable(runnable), namePrefix + threadNumber.getAndIncrement(), 0);
    thread.setDaemon(true);
    thread.setPriority(Thread.NORM_PRIORITY);
    return thread;
  }

  private Runnable getThreadRunnable(Runnable target) {
    if (exitCallback == null) {
      return target;
    } else {
      return new ExitCallbackRunnable(target);
    }
  }

  private class ExitCallbackRunnable implements Runnable {
    private final Runnable original;

    private ExitCallbackRunnable(Runnable original) {
      this.original = original;
    }

    @Override
    public void run() {
      try {
        if (original != null) {
          original.run();
        }
      } finally {
        wrapExitCallback();
      }
    }

    private void wrapExitCallback() {
      boolean wasInterrupted = Thread.interrupted();

      try {
        exitCallback.run();
      } catch (Throwable throwable) {
        log.error("Thread exit notification threw an exception.", throwable);
      } finally {
        if (wasInterrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}
