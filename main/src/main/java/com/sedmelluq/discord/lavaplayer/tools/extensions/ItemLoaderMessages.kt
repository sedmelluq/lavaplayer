package com.sedmelluq.discord.lavaplayer.tools.extensions

import kotlinx.coroutines.Job
import com.sedmelluq.discord.lavaplayer.track.loader.message.ItemLoaderMessage
import com.sedmelluq.discord.lavaplayer.track.loader.message.ItemLoaderMessages

/**
 *
 */
inline fun <reified E : ItemLoaderMessage> ItemLoaderMessages.on(noinline block: suspend E.() -> Unit): Job {
    return on(E::class, block)
}
