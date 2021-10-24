package com.sedmelluq.discord.lavaplayer.track.loader

import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.loader.message.ItemLoaderMessage
import com.sedmelluq.discord.lavaplayer.track.loader.message.ItemLoaderMessages
import kotlin.reflect.KClass

data class LoaderState(
    /**
     * The audio reference that holds the identifier that a specific source manager should be able
     * to find a track with.
     */
    val reference: AudioReference,
    /**
     * Used for communicating between the item loader and source managers.
     */
    val messages: ItemLoaderMessages
) {
    companion object {
        val NONE = LoaderState(AudioReference.NO_TRACK, object : ItemLoaderMessages {
            override fun send(message: ItemLoaderMessage): Boolean = false
            override fun <T : ItemLoaderMessage> on(clazz: KClass<T>, block: suspend T.() -> Unit): Job = NonCancellable
            override fun shutdown() = Unit
        })
    }
}
