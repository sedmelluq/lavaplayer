package com.sedmelluq.discord.lavaplayer.tools.extensions

import com.sedmelluq.discord.lavaplayer.source.ItemSourceManager
import com.sedmelluq.discord.lavaplayer.source.common.SourceRegistry


/**
 * Shortcut for accessing a specific source manager.
 * @param T The source manager class.
 * @return The source manager of the specified class, or null if not registered.
 */
inline fun <reified T : ItemSourceManager> SourceRegistry.source(): T? =
    source(T::class.java)
