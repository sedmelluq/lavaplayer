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

    /**
     * Loads a track (collection) with the supplied identifier.
     *
     * @return [ItemLoadResult]
     */
    suspend fun load(): ItemLoadResult

    /**
     * Schedules loading a track (collection) with the specified identifier.
     */
    fun loadAsync(): Deferred<ItemLoadResult>
}
