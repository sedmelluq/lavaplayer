package com.sedmelluq.discord.lavaplayer.source.getyarn

import com.sedmelluq.discord.lavaplayer.source.ItemSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.io.*
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools.NoRedirectsStrategy
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.loader.LoaderState
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import org.jsoup.Jsoup
import java.io.DataInput
import java.io.IOException
import java.util.regex.Pattern

/**
 * Audio source manager which detects getyarn.io tracks by URL.
 */
class GetyarnItemSourceManager : HttpConfigurable, ItemSourceManager {
    private val httpInterfaceManager: HttpInterfaceManager = ThreadLocalHttpInterfaceManager(
        HttpClientTools
            .createSharedCookiesHttpBuilder()
            .setRedirectStrategy(NoRedirectsStrategy()),
        HttpClientTools.DEFAULT_REQUEST_CONFIG
    )

    val httpInterface: HttpInterface
        get() = httpInterfaceManager.get()

    override val sourceName: String
        get() = "getyarn.io"

    override suspend fun loadItem(state: LoaderState, reference: AudioReference): AudioItem? {
        val m = GETYARN_REGEX.matcher(reference.identifier)
        return if (!m.matches()) {
            null
        } else {
            extractVideoUrlFromPage(reference)
        }
    }

    override fun isTrackEncodable(track: AudioTrack): Boolean {
        return true
    }

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack? {
        return GetyarnAudioTrack(trackInfo, this)
    }

    override fun configureRequests(configurator: RequestConfigurator) {
        httpInterfaceManager.configureRequests(configurator)
    }

    override fun configureBuilder(configurator: BuilderConfigurator) {
        httpInterfaceManager.configureBuilder(configurator)
    }

    private fun createTrack(trackInfo: AudioTrackInfo): AudioTrack {
        return GetyarnAudioTrack(trackInfo, this)
    }

    private fun extractVideoUrlFromPage(reference: AudioReference): AudioTrack {
        try {
            httpInterface.execute(HttpGet(reference.identifier)).use { response ->
                val html = EntityUtils.toString(response.entity, Charsets.UTF_8)
                val document = Jsoup.parse(html)
                val trackInfo = AudioTrackInfo {
                    uri = reference.uri
                    author = "Unknown"
                    identifier = document.selectFirst("meta[property=og:video:secure_url]")!!.attr("content")
                    title = document.selectFirst("meta[property=og:title]")!!.attr("content")
                }

                return createTrack(trackInfo)
            }
        } catch (e: IOException) {
            throw FriendlyException("Failed to load info for yarn clip", FriendlyException.Severity.SUSPICIOUS, null)
        }
    }

    companion object {
        private val GETYARN_REGEX = Pattern.compile("(?:http://|https://(?:www\\.)?)?getyarn\\.io/yarn-clip/(.*)")
    }
}
