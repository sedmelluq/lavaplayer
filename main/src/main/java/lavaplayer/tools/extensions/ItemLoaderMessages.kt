package lavaplayer.tools.extensions

import kotlinx.coroutines.Job
import lavaplayer.track.loader.message.ItemLoaderMessage
import lavaplayer.track.loader.message.ItemLoaderMessages

/**
 *
 */
inline fun <reified E : ItemLoaderMessage> ItemLoaderMessages.on(noinline block: suspend E.() -> Unit): Job {
    return on(E::class, block)
}
