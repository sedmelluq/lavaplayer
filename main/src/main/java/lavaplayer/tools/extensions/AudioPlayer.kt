package lavaplayer.tools.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import lavaplayer.manager.AudioPlayer
import lavaplayer.manager.event.AudioEvent
import lavaplayer.manager.event.AudioEventListener
import org.slf4j.LoggerFactory

@PublishedApi
internal val aplog = LoggerFactory.getLogger("AudioPlayer.on")

/**
 * Add a listener to events from this player.
 *
 * @param listener New listener
 */
fun AudioPlayer.addListener(listener: AudioEventListener): Job {
    return on<AudioEvent>(this) { listener.onEvent(this) }
}

inline fun <reified T : AudioEvent> AudioPlayer.on(scope: CoroutineScope = this, noinline block: suspend T.() -> Unit): Job {
    return events.buffer(UNLIMITED).filterIsInstance<T>()
        .onEach { event ->
            launch {
                event
                    .runCatching { block() }
                    .onFailure { aplog.error("Error occurred while handling event ${event::class.qualifiedName}", it) }
            }
        }
        .launchIn(scope)
}
