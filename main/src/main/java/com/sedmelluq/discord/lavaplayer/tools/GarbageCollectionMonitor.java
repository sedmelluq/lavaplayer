package com.sedmelluq.discord.lavaplayer.tools;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.sun.management.GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION;
import static com.sun.management.GarbageCollectionNotificationInfo.from;

/**
 * Garbage collection monitor which records all GC pause lengths and logs them. In case the GC pause statistics are
 * considered bad for latency, the statistics are logged at a warning level.
 */
public class GarbageCollectionMonitor implements NotificationListener, Runnable {
  private static final Logger log = LoggerFactory.getLogger(GarbageCollectionMonitor.class);

  private static final long REPORTING_FREQUENCY = TimeUnit.MINUTES.toMillis(2);
  private static final long[] BUCKETS = new long[] { 2000, 500, 200, 50, 20, 0 };

  private final ScheduledExecutorService reportingExecutor;
  private final int[] bucketCounters;
  private final AtomicBoolean enabled;
  private final AtomicReference<ScheduledFuture<?>> executorFuture;

  /**
   * Create an instance of GC monitor. Does nothing until enabled.
   * @param reportingExecutor Executor to use for scheduling reporting task
   */
  public GarbageCollectionMonitor(ScheduledExecutorService reportingExecutor) {
    this.reportingExecutor = reportingExecutor;
    bucketCounters = new int[BUCKETS.length];
    enabled = new AtomicBoolean();
    executorFuture = new AtomicReference<>();
  }

  /**
   * Enable GC monitoring and reporting.
   */
  public void enable() {
    if (enabled.compareAndSet(false, true)) {
      registerBeanListener();

      executorFuture.set(reportingExecutor.scheduleAtFixedRate(this, REPORTING_FREQUENCY, REPORTING_FREQUENCY, TimeUnit.MILLISECONDS));

      log.info("GC monitoring enabled, reporting results every 2 minutes.");
    }
  }

  /**
   * Disable GC monitoring and reporting.
   */
  public void disable() {
    if (enabled.compareAndSet(true, false)) {
      unregisterBeanListener();

      ScheduledFuture<?> scheduledTask = executorFuture.getAndSet(null);
      if (scheduledTask != null) {
        scheduledTask.cancel(false);
      }

      log.info("GC monitoring disabled.");
    }
  }

  private void registerBeanListener() {
    for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
      if (gcBean instanceof NotificationEmitter) {
        ((NotificationEmitter) gcBean).addNotificationListener(this, null, gcBean);
      }
    }
  }

  private void unregisterBeanListener() {
    for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
      if (gcBean instanceof NotificationEmitter) {
        try {
          ((NotificationEmitter) gcBean).removeNotificationListener(this);
        } catch (ListenerNotFoundException e) {
          log.debug("No listener found on bean {}, should have been there.", gcBean, e);
        }
      }
    }
  }

  private void registerPause(long duration) {
    synchronized (bucketCounters) {
      for (int i = 0; i < bucketCounters.length; i++) {
        if (duration >= BUCKETS[i]) {
          bucketCounters[i]++;
          break;
        }
      }
    }
  }

  @Override
  public void handleNotification(Notification notification, Object handback) {
    if (GARBAGE_COLLECTION_NOTIFICATION.equals(notification.getType())) {
      GarbageCollectionNotificationInfo notificationInfo = from((CompositeData) notification.getUserData());
      GcInfo info = notificationInfo.getGcInfo();

      if (info != null && !"No GC".equals(notificationInfo.getGcCause())) {
        registerPause(info.getDuration());
      }
    }
  }

  @Override
  public void run() {
    StringBuilder statistics = new StringBuilder();
    boolean hasBadLatency;

    synchronized (bucketCounters) {
      hasBadLatency = bucketCounters[3] > 1 || bucketCounters[2] > 0;

      for (int i = bucketCounters.length - 1; i >= 0; i--) {
        statistics.append(String.format("[Bucket %d = %d] ", BUCKETS[i], bucketCounters[i]));
        bucketCounters[i] = 0;
      }
    }

    if (hasBadLatency) {
      log.warn("Suspicious GC results for the last 2 minutes: {}", statistics);
    } else {
      log.debug("GC results for the last 2 minutes: {}", statistics);
    }
  }
}
