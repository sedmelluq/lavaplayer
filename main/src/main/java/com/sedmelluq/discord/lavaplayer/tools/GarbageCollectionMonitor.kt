package com.sedmelluq.discord.lavaplayer.tools

import com.sun.management.GarbageCollectionNotificationInfo
import kotlinx.atomicfu.atomic
import mu.KotlinLogging
import java.lang.management.ManagementFactory
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.management.ListenerNotFoundException
import javax.management.Notification
import javax.management.NotificationEmitter
import javax.management.NotificationListener
import javax.management.openmbean.CompositeData

/**
 * Garbage collection monitor which records all GC pause lengths and logs them. In case the GC pause statistics are
 * considered bad for latency, the statistics are logged at a warning level.
 */
class GarbageCollectionMonitor(private val reportingExecutor: ScheduledExecutorService) : NotificationListener, Runnable {
    companion object {
        private val log = KotlinLogging.logger { }
        private val REPORTING_FREQUENCY = TimeUnit.MINUTES.toMillis(2)
        private val BUCKETS = longArrayOf(2000, 500, 200, 50, 20, 0)
    }

    private val bucketCounters = IntArray(BUCKETS.size)
    private val enabled = atomic(false)
    private var executorFuture by atomic<ScheduledFuture<*>?>(null)

    /**
     * Enable GC monitoring and reporting.
     */
    fun enable() {
        if (enabled.compareAndSet(false, true)) {
            registerBeanListener()
            executorFuture = reportingExecutor.scheduleAtFixedRate(
                this,
                REPORTING_FREQUENCY,
                REPORTING_FREQUENCY,
                TimeUnit.MILLISECONDS
            )
            log.info { "GC monitoring enabled, reporting results every 2 minutes." }
        }
    }

    /**
     * Disable GC monitoring and reporting.
     */
    fun disable() {
        if (enabled.compareAndSet(true, false)) {
            unregisterBeanListener()

            executorFuture?.cancel(false)
            executorFuture = null

            log.info { "GC monitoring disabled." }
        }
    }

    override fun handleNotification(notification: Notification, handback: Any) {
        if (GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION == notification.type) {
            val notificationInfo = GarbageCollectionNotificationInfo.from(notification.userData as CompositeData)
            val info = notificationInfo.gcInfo
            if (info != null && "No GC" != notificationInfo.gcCause) {
                registerPause(info.duration)
            }
        }
    }

    override fun run() {
        val statistics = StringBuilder()

        var hasBadLatency: Boolean
        synchronized(bucketCounters) {
            hasBadLatency = bucketCounters[0] + bucketCounters[1] + bucketCounters[2] > 0
            for (i in bucketCounters.indices.reversed()) {
                statistics.append(String.format("[Bucket %d = %d] ", BUCKETS[i], bucketCounters[i]))
                bucketCounters[i] = 0
            }
        }

        if (hasBadLatency) {
            log.warn { "Suspicious GC results for the last 2 minutes: $statistics" }
        } else {
            log.debug { "GC results for the last 2 minutes: $statistics" }
        }
    }

    private fun registerBeanListener() {
        ManagementFactory.getGarbageCollectorMXBeans()
            .filterIsInstance<NotificationEmitter>()
            .forEach { bean -> bean.addNotificationListener(this, null, bean) }
    }

    private fun unregisterBeanListener() {
        ManagementFactory.getGarbageCollectorMXBeans()
            .filterIsInstance<NotificationEmitter>()
            .forEach { bean ->
                try {
                    bean.removeNotificationListener(this)
                } catch (e: ListenerNotFoundException) {
                    log.debug(e) { "No listener found on bean $bean, should have been there." }
                }
            }
    }

    private fun registerPause(duration: Long) {
        synchronized(bucketCounters) {
            for (i in bucketCounters.indices) {
                if (duration >= BUCKETS[i]) {
                    bucketCounters[i]++
                    break
                }
            }
        }
    }
}
