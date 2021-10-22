package lavaplayer.source.vimeo

import lavaplayer.container.mpeg.MpegAudioTrack
import lavaplayer.tools.FriendlyException
import lavaplayer.tools.extensions.decodeJson
import lavaplayer.tools.io.HttpClientTools
import lavaplayer.tools.io.HttpInterface
import lavaplayer.tools.io.PersistentHttpStream
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackInfo
import lavaplayer.track.DelegatedAudioTrack
import lavaplayer.track.playback.LocalAudioTrackExecutor
import mu.KotlinLogging
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import java.io.IOException
import java.net.URI

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
        private val log = KotlinLogging.logger { }
    }

    override fun makeShallowClone(): AudioTrack =
        VimeoAudioTrack(info, sourceManager)

    @Throws(Exception::class)
    override fun process(executor: LocalAudioTrackExecutor) {
        sourceManager.httpInterface.use { httpInterface ->
            val playbackUrl = loadPlaybackUrl(httpInterface)
            PersistentHttpStream(httpInterface, URI(playbackUrl), null).use { stream ->
                log.debug { "Starting Vimeo track from URL: $playbackUrl" }
                processDelegate(MpegAudioTrack(info, stream), executor)
            }
        }
    }

    @Throws(IOException::class)
    private fun loadPlaybackUrl(httpInterface: HttpInterface): String {
        val config = loadPlayerConfig(httpInterface)
            ?: throw FriendlyException("Track information not present on the page.", FriendlyException.Severity.SUSPICIOUS, null)

        val player = loadTrackConfig(httpInterface, config.player.configUrl)
        return player.request.files.best.url
    }

    @Throws(IOException::class)
    private fun loadPlayerConfig(httpInterface: HttpInterface): VimeoClipPage? {
        httpInterface.execute(HttpGet(info.identifier)).use { response ->
            val statusCode = response.statusLine.statusCode
            if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw FriendlyException("Server responded with an error.", FriendlyException.Severity.SUSPICIOUS, IllegalStateException("Response code for player config is $statusCode"))
            }

            val pageContent = EntityUtils.toString(response.entity, Charsets.UTF_8)
            return sourceManager.loadConfigJsonFromPageContent(pageContent)
        }
    }

    @Throws(IOException::class)
    private fun loadTrackConfig(httpInterface: HttpInterface, playerUrl: String?): VimeoPlayer {
        httpInterface.execute(HttpGet(playerUrl)).use { response ->
            val statusCode = response.statusLine.statusCode
            if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw FriendlyException(
                    "Server responded with an error.", FriendlyException.Severity.SUSPICIOUS,
                    IllegalStateException("Response code for track access info is $statusCode")
                )
            }

            return response.entity.content.decodeJson()
        }
    }
}
