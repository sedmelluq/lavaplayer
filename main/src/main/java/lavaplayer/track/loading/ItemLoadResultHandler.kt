package lavaplayer.track.loading

fun interface ItemLoadResultHandler {
    /**
     * Handles an item load result.
     *
     * @param result The load result.
     */
    suspend fun handle(result: ItemLoadResult)
}
