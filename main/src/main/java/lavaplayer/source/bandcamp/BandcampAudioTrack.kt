package lavaplayer.source.bandcamp

import lavaplayer.track.AudioTrackInfo
import lavaplayer.track.DelegatedAudioTrack
import kotlin.Throws
import lavaplayer.track.playback.LocalAudioTrackExecutor
import lavaplayer.tools.io.PersistentHttpStream
import lavaplayer.container.mp3.Mp3AudioTrack
import java.io.IOException
import lavaplayer.tools.io.HttpInterface
import org.apache.http.client.methods.HttpGet
import lavaplayer.tools.io.HttpClientTools
import lavaplayer.track.AudioTrack
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.net.URI
import java.nio.charset.StandardCharsets

/**
 * Audio track that handles processing Bandcamp tracks.
 *
 * @param trackInfo     Track info
 * @param sourceManager Source manager which was used to find this track
 */
class BandcampAudioTrack(
    trackInfo: AudioTrackInfo,
    override val sourceManager: BandcampItemSourceManager
) : DelegatedAudioTrack(trackInfo) {
    companion object {
        private val log = LoggerFactory.getLogger(BandcampAudioTrack::class.java)
    }

    @Throws(Exception::class)
    override fun process(executor: LocalAudioTrackExecutor) {
        sourceManager.httpInterface.use { httpInterface ->
            log.debug("Loading Bandcamp track page from URL: {}", info.identifier)

            val trackMediaUrl = getTrackMediaUrl(httpInterface)
            PersistentHttpStream(httpInterface, URI(trackMediaUrl), null).use { stream ->
                log.debug("Starting Bandcamp track from URL: {}", trackMediaUrl)
                processDelegate(Mp3AudioTrack(info, stream), executor)
            }
        }
    }

    @Throws(IOException::class)
    private fun getTrackMediaUrl(httpInterface: HttpInterface): String {
        httpInterface.execute(HttpGet(info.identifier)).use { response ->
            HttpClientTools.assertSuccessWithContent(response, "track page")
            val responseText = IOUtils.toString(response.entity.content, StandardCharsets.UTF_8)
            val trackInfo = sourceManager.readTrackListInformation(responseText)
            return trackInfo["trackinfo"].index(0)["file"]["mp3-128"].safeText
        }
    }

    override fun makeShallowClone(): AudioTrack =
        BandcampAudioTrack(info, sourceManager)
}
