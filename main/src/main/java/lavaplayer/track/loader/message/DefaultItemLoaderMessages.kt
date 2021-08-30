package lavaplayer.track.loader.message

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

class DefaultItemLoaderMessages : ItemLoaderMessages, CoroutineScope {
    companion object {
        @PublishedApi
        internal val log = LoggerFactory.getLogger(DefaultItemLoaderMessages::class.java)
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + Job()

    var active = false
    val events: SharedFlow<ItemLoaderMessage>
        get() = eventFlow

    private val eventFlow: MutableSharedFlow<ItemLoaderMessage> by lazy {
        MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)
    }

    override fun <T : ItemLoaderMessage> on(clazz: KClass<T>, block: suspend T.() -> Unit): Job {
        active = true
        return (events
            .buffer(Channel.UNLIMITED)
            .filter { clazz.isInstance(it) } as Flow<T>)
            .onEach { event ->
                launch {
                    event
                        .runCatching { block(event) }
                        .onFailure { log.error("Error occurred while handling audio reference loader message", it) }
                }
            }
            .launchIn(this)
    }

    /**
     * Sends a new message.
     *
     * @param message A [ItemLoaderMessage] to send.
     */
    override fun send(message: ItemLoaderMessage): Boolean {
        if (!active) {
            return false
        }

        return eventFlow.tryEmit(message)
    }

    override fun shutdown() {
        cancel()
    }
}