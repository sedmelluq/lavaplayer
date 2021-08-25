package lavaplayer.source.vimeo

import lavaplayer.tools.JsonBrowser.Companion.parse
import lavaplayer.track.AudioTrackInfo
import lavaplayer.track.DelegatedAudioTrack
import kotlin.Throws
import lavaplayer.track.playback.LocalAudioTrackExecutor
import lavaplayer.tools.io.PersistentHttpStream
import lavaplayer.container.mpeg.MpegAudioTrack
import java.io.IOException
import lavaplayer.tools.io.HttpInterface
import lavaplayer.tools.JsonBrowser
import lavaplayer.tools.FriendlyException
import org.apache.http.client.methods.HttpGet
import lavaplayer.tools.io.HttpClientTools
import java.lang.IllegalStateException
import lavaplayer.track.AudioTrack
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.net.URI
import java.nio.charset.StandardCharsets

/**
 * Audio track that handles processing Vimeo tracks.
 *
 * @param trackInfo     Track info
 * @param sourceManager Source manager which was used to find this track
 */
class VimeoAudioTrack(
    trackInfo: AudioTrackInfo,
    override val sourceManager: VimeoItemSourceManager
) : DelegatedAudioTrack(trackInfo) {
    companion object {
        private val log = LoggerFactory.getLogger(VimeoAudioTrack::class.java)
    }

    @Throws(Exception::class)
    override fun process(executor: LocalAudioTrackExecutor) {
        sourceManager.httpInterface.use { httpInterface ->
            val playbackUrl = loadPlaybackUrl(httpInterface)
            PersistentHttpStream(httpInterface, URI(playbackUrl), null).use { stream ->
                log.debug("Starting Vimeo track from URL: {}", playbackUrl)
                processDelegate(MpegAudioTrack(info, stream), executor)
            }
        }
    }

    @Throws(IOException::class)
    private fun loadPlaybackUrl(httpInterface: HttpInterface): String? {
        val config = loadPlayerConfig(httpInterface)
            ?: throw FriendlyException(
                "Track information not present on the page.",
                FriendlyException.Severity.SUSPICIOUS,
                null
            )

        val trackConfigUrl = config["player"]["config_url"].text
        val trackConfig = loadTrackConfig(httpInterface, trackConfigUrl)
        return trackConfig["request"]["files"]["progressive"].index(0)["url"].text
    }

    @Throws(IOException::class)
    private fun loadPlayerConfig(httpInterface: HttpInterface): JsonBrowser? {
        httpInterface.execute(HttpGet(info.identifier)).use { response ->
            val statusCode = response.statusLine.statusCode
            if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw FriendlyException(
                    "Server responded with an error.", FriendlyException.Severity.SUSPICIOUS,
                    IllegalStateException("Response code for player config is $statusCode")
                )
            }

            return sourceManager.loadConfigJsonFromPageContent(
                IOUtils.toString(
                    response.entity.content,
                    StandardCharsets.UTF_8
                )
            )
        }
    }

    @Throws(IOException::class)
    private fun loadTrackConfig(httpInterface: HttpInterface, trackAccessInfoUrl: String?): JsonBrowser {
        httpInterface.execute(HttpGet(trackAccessInfoUrl)).use { response ->
            val statusCode = response.statusLine.statusCode
            if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw FriendlyException(
                    "Server responded with an error.", FriendlyException.Severity.SUSPICIOUS,
                    IllegalStateException("Response code for track access info is $statusCode")
                )
            }

            return parse(response.entity.content)
        }
    }

    override fun makeShallowClone(): AudioTrack =
        VimeoAudioTrack(info, sourceManager)
}
