package lavaplayer.track.loader

import kotlinx.coroutines.Deferred

interface ItemLoader {
    /**
     * The current state of this loader.
     */
    val state: LoaderState

    /**
     * The result handler.
     */
    var resultHandler: ItemLoadResultHandler?

    suspend fun load(): ItemLoadResult =
        loadAsync().await()

    /**
     * Schedules loading a track (collection) with the specified identifier.
     */
    fun loadAsync(): Deferred<ItemLoadResult>
}
