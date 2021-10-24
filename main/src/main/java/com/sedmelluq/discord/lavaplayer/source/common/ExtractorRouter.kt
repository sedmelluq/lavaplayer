package com.sedmelluq.discord.lavaplayer.source.common

import com.sedmelluq.discord.lavaplayer.track.AudioItem
import java.util.regex.Matcher

fun interface ExtractorRouter<R : LinkRoutes> {
    /**
     * @param routes The [LinkRoutes] to use
     * @param context The context
     */
    suspend fun extract(routes: R, context: ExtractorContext): AudioItem?
}

/**
 * @param identifier The identifier that was used.
 * @param matcher The pattern matcher
 */
data class ExtractorContext(val identifier: String, val matcher: Matcher)
