package com.sedmelluq.discord.lavaplayer.source.soundcloud

import com.sedmelluq.discord.lavaplayer.source.common.Extractor
import com.sedmelluq.discord.lavaplayer.source.common.ExtractorContext
import com.sedmelluq.discord.lavaplayer.source.common.LinkRouter
import com.sedmelluq.discord.lavaplayer.track.AudioItem

class SoundCloudLinkRouter : LinkRouter.UsingExtractors<SoundCloudLinkRoutes> {
    companion object {
        private const val PROTOCOL_REGEX = """(?:https?://)?"""
        private const val SUBDOMAIN_REGEX = """(?:www\.|m\.)?"""
        private const val QUERY_REGEX = """/?(?:\?.*)?"""

        private const val DOMAIN_REGEX = """soundcloud\.com"""
        private const val DOMAIN_EXTENDED_REGEX = """soundcloud\.(app\.goo\.gl|com)"""

        private const val BASE_REGEX = """^$PROTOCOL_REGEX$SUBDOMAIN_REGEX$DOMAIN_REGEX"""
        private const val TRACK_URL_REGEX =
            """^$PROTOCOL_REGEX$SUBDOMAIN_REGEX$DOMAIN_EXTENDED_REGEX/([\d\w_-]+)/([\d\w_-]+)$QUERY_REGEX$"""
        private const val UNLISTED_URL_REGEX = """^$BASE_REGEX/([\d\w_-]+)/([\d\w_-]+)/s-([\d\w_-]+)$QUERY_REGEX$"""
        private const val LIKED_URL_REGEX = """^$BASE_REGEX/([\d\w_-]+)/likes$QUERY_REGEX$"""
        private const val PLAYLIST_URL_REGEX = """^$BASE_REGEX/([\d\w_-]+)/sets/([\d\w_-]+)$QUERY_REGEX$"""

        private const val SEARCH_PREFIX = "scsearch"

        private const val BASIC_SEARCH_REGEX = """^$SEARCH_PREFIX:\s*(.*)\s*$"""
        private const val ADVANCED_SEARCH_REGEX = """^$SEARCH_PREFIX\[(\d{1,9}),\s?(\d{1,9})]:\s*(.*)\s*$"""
    }

    override val extractors = listOf(
        /* search */
        Extractor(ADVANCED_SEARCH_REGEX.toPattern(), ::routeAdvancedSearch),
        Extractor(BASIC_SEARCH_REGEX.toPattern(), ::routeBasicSearch),
        /* other */
        Extractor(LIKED_URL_REGEX.toPattern(), ::routeLikedTracks),
        Extractor(PLAYLIST_URL_REGEX.toPattern(), ::routeSet),
        Extractor(TRACK_URL_REGEX.toPattern(), ::routeTrack),
        Extractor(UNLISTED_URL_REGEX.toPattern(), ::routeTrack),
    )

    private suspend fun routeAdvancedSearch(routes: SoundCloudLinkRoutes, context: ExtractorContext): AudioItem? {
        val offset = context.matcher.group(1).toInt()
        val limit = context.matcher.group(2).toInt()
        val query = context.matcher.group(3)
        return routes.search(query, offset, limit)
    }

    private suspend fun routeBasicSearch(routes: SoundCloudLinkRoutes, context: ExtractorContext): AudioItem? {
        val query = context.matcher.group(1)
        return routes.search(query, 0, SoundCloudItemSourceManager.DEFAULT_SEARCH_RESULTS)
    }

    private suspend fun routeSet(routes: SoundCloudLinkRoutes, context: ExtractorContext): AudioItem? {
        return routes.set(context.identifier)
    }

    private suspend fun routeTrack(routes: SoundCloudLinkRoutes, context: ExtractorContext): AudioItem? {
        if (context.matcher.group(2) == "likes") {
            return routeLikedTracks(routes, context)
        }

        return routes.track(context.identifier)
    }

    private suspend fun routeLikedTracks(routes: SoundCloudLinkRoutes, context: ExtractorContext): AudioItem? {
        return routes.liked(context.identifier)
    }
}
