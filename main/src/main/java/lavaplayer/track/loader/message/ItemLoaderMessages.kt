package lavaplayer.track.loader.message

import kotlinx.coroutines.Job
import kotlin.reflect.KClass

interface ItemLoaderMessages {
    fun <T : ItemLoaderMessage> on(clazz: KClass<T>, block: suspend T.() -> Unit): Job
    fun send(message: ItemLoaderMessage): Boolean
    fun shutdown()
}
