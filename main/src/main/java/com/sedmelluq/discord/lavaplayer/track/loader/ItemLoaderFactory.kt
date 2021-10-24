package com.sedmelluq.discord.lavaplayer.track.loader

import com.sedmelluq.discord.lavaplayer.track.AudioReference

interface ItemLoaderFactory {
    /**
     * The number of threads used for processing item load requests.
     */
    var itemLoaderPoolSize: Int

    /**
     * Creates a new item loader for the provided identifier.
     *
     * @param identifier The reference identifier.
     */
    fun createItemLoader(identifier: String): ItemLoader =
        createItemLoader(AudioReference(identifier, null))

    /**
     * Creates a new item loader for the provided audio reference.
     *
     * @param reference The audio reference.
     */
    fun createItemLoader(reference: AudioReference): ItemLoader

    /**
     * Shutdown this item loader factory.
     */
    fun shutdown()
}
