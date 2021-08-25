package lavaplayer.track.loading

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

class ItemLoaderMessages : CoroutineScope {
    companion object {
        @PublishedApi
        internal val log = LoggerFactory.getLogger(ItemLoaderMessages::class.java)
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + Job()

    var communicating = false
    val events: SharedFlow<ItemLoaderMessage> get() = eventFlow

    private val eventFlow: MutableSharedFlow<ItemLoaderMessage> by lazy {
        MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)
    }

    /**
     *
     */
    inline fun <reified E : ItemLoaderMessage> on(
        scope: CoroutineScope = this,
        noinline block: suspend E.() -> Unit
    ): Job {
        communicating = true
        return events
            .buffer(Channel.UNLIMITED)
            .filterIsInstance<E>()
            .onEach { event ->
                launch {
                    event
                        .runCatching { block(event) }
                        .onFailure { log.error("Error occurred while handling audio reference loader message", it) }
                }
            }
            .launchIn(scope)
    }

    /**
     * Sends a new message.
     *
     * @param message A [ItemLoaderMessage] to send.
     */
    fun send(message: ItemLoaderMessage): Boolean {
        if (!communicating) {
            return false
        }

        return eventFlow.tryEmit(message)
    }
}
