package com.sedmelluq.discord.lavaplayer.track.loader

fun interface ItemLoadResultHandler {
    /**
     * Handles an item load result.
     *
     * @param result The load result.
     */
    suspend fun handle(result: ItemLoadResult)
}
