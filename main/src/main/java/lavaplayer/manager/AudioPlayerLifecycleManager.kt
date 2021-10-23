package lavaplayer.manager

import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic
import lavaplayer.manager.event.AudioEvent
import lavaplayer.manager.event.AudioEventListener
import lavaplayer.manager.event.TrackEndEvent
import lavaplayer.manager.event.TrackStartEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Triggers cleanup checks on all active audio players at a fixed interval.
 *
 * @param scheduler        Scheduler to use for the cleanup check task
 * @param cleanupThreshold Threshold for player cleanup
 */
class AudioPlayerLifecycleManager(
    private val scheduler: ScheduledExecutorService,
    private val cleanupThreshold: AtomicLong
) : Runnable, AudioEventListener {
    companion object {
        private const val CHECK_INTERVAL: Long = 10000
    }

    private val activePlayers = ConcurrentHashMap<AudioPlayer, AudioPlayer>()
    private val scheduledTask = atomic<ScheduledFuture<*>?>(null)

    /**
     * Initialise the scheduled task.
     */
    init {
        val task = scheduler.scheduleAtFixedRate(this, CHECK_INTERVAL, CHECK_INTERVAL, TimeUnit.MILLISECONDS)
        if (!scheduledTask.compareAndSet(null, task)) {
            task.cancel(false)
        }
    }

    /**
     * Stop the scheduled task.
     */
    fun shutdown() {
        scheduledTask.getAndSet(null)?.cancel(false)
    }

    override suspend fun onEvent(event: AudioEvent) {
        when (event) {
            is TrackStartEvent -> activePlayers[event.player] = event.player
            is TrackEndEvent -> activePlayers.remove(event.player)
        }
    }

    override fun run() {
        for (player in activePlayers.keys) {
            player.checkCleanup(cleanupThreshold.value)
        }
    }
}
