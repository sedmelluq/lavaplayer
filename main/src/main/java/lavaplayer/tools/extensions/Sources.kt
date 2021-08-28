package lavaplayer.tools.extensions

import lavaplayer.source.ItemSourceManager
import lavaplayer.source.SourceRegistry


/**
 * Shortcut for accessing a specific source manager.
 * @param T The source manager class.
 * @return The source manager of the specified class, or null if not registered.
 */
inline fun <reified T : ItemSourceManager> SourceRegistry.source(): T? =
    source(T::class.java)
