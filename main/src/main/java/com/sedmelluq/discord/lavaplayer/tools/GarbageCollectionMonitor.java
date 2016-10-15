package com.sedmelluq.discord.lavaplayer.tools;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

  /**
   * Create an instance of GC monitor. Does nothing until enabled.
   */
  public GarbageCollectionMonitor() {
    reportingExecutor = Executors.newScheduledThreadPool(0, new DaemonThreadFactory("gc-report"));
    bucketCounters = new int[BUCKETS.length];
    enabled = new AtomicBoolean();
  }

  /**
   * Enable GC monitoring and reporting.
   */
  public void enable() {
    if (enabled.compareAndSet(false, true)) {
      for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
        if (gcBean instanceof NotificationEmitter) {
          ((NotificationEmitter) gcBean).addNotificationListener(this, null, gcBean);
        }
      }

      reportingExecutor.scheduleAtFixedRate(this, REPORTING_FREQUENCY, REPORTING_FREQUENCY, TimeUnit.MILLISECONDS);

      log.info("GC monitoring enabled, reporting results every 2 minutes.");
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
      log.warn("Suspicious GC results for the last 2 minutes: {}", statistics.toString());
    } else {
      log.debug("GC results for the last 2 minutes: {}", statistics.toString());
    }
  }
}
