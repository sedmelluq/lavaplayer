package com.sedmelluq.discord.lavaplayer.source.nico

import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import mu.KotlinLogging
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.util.EntityUtils
import java.io.IOException
import java.net.URI
import java.nio.charset.StandardCharsets

/**
 * Audio track that handles processing NicoNico tracks.
 *
 * @param trackInfo     Track info
 * @param sourceManager Source manager which was used to find this track
 */
class NicoAudioTrack(
    trackInfo: AudioTrackInfo,
    override val sourceManager: NicoItemSourceManager
) : DelegatedAudioTrack(trackInfo) {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    @Throws(Exception::class)
    override fun process(executor: LocalAudioTrackExecutor) {
        sourceManager.checkLoggedIn()
        sourceManager.httpInterface.use { httpInterface ->
            loadVideoMainPage(httpInterface)
            val playbackUrl = loadPlaybackUrl(httpInterface).also {
                log.debug { "Starting NicoNico track from URL: $it" }
            }

            PersistentHttpStream(httpInterface, URI(playbackUrl), null).use { stream ->
                processDelegate(MpegAudioTrack(info, stream), executor)
            }
        }
    }

    @Throws(IOException::class)
    private fun loadVideoMainPage(httpInterface: HttpInterface) {
        val request = HttpGet("http://www.nicovideo.jp/watch/" + info.identifier)
        httpInterface.execute(request).use { response ->
            val statusCode = response.statusLine.statusCode
            if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw IOException("Unexpected status code from video main page: $statusCode")
            }

            EntityUtils.consume(response.entity)
        }
    }

    @Throws(IOException::class)
    private fun loadPlaybackUrl(httpInterface: HttpInterface): String {
        val request = HttpGet("http://flapi.nicovideo.jp/api/getflv/" + info.identifier)
        httpInterface.execute(request).use { response ->
            val statusCode = response.statusLine.statusCode
            if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw IOException("Unexpected status code from playback parameters page: $statusCode")
            }

            val text = EntityUtils.toString(response.entity)
            val format = DataFormatTools.convertToMapLayout(URLEncodedUtils.parse(text, StandardCharsets.UTF_8))
            return format["url"]!!
        }
    }

    override fun makeShallowClone(): AudioTrack =
        NicoAudioTrack(info, sourceManager)
}
