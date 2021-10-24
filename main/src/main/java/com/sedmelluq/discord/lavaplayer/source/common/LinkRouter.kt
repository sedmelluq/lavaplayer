package com.sedmelluq.discord.lavaplayer.source.common

import com.sedmelluq.discord.lavaplayer.track.AudioItem

interface LinkRouter<R : LinkRoutes> {
    /**
     * Finds a route for the supplied [identifier] in [routes].
     *
     * @param identifier
     *   The identifier to find a route for.
     *
     * @param routes
     *   The [LinkRoutes] instance to use.
     *
     * @return The found [AudioItem], or null if nothing was found.
     */
    suspend fun find(identifier: String, routes: R): AudioItem?

    interface UsingExtractors<R : LinkRoutes> : LinkRouter<R> {
        /**
         * The extractors to use.
         */
        val extractors: List<Extractor<R>>

        override suspend fun find(identifier: String, routes: R): AudioItem? {
            return extractors.firstNotNullOfOrNull { extractor ->
                val matcher = extractor.pattern
                    .matcher(identifier)
                    .takeIf { it.find() }

                matcher?.let { extractor.router.extract(routes, ExtractorContext(identifier, matcher)) }
            }
        }
    }
}
