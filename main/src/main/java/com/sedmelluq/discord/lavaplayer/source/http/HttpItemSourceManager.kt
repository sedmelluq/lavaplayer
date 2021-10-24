package com.sedmelluq.discord.lavaplayer.source.http

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDescriptor
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints.Companion.from
import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry
import com.sedmelluq.discord.lavaplayer.source.ProbingItemSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.Units
import com.sedmelluq.discord.lavaplayer.tools.io.*
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools.NoRedirectsStrategy
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoBuilder.Companion.create
import com.sedmelluq.discord.lavaplayer.track.loader.LoaderState
import org.apache.http.HttpStatus
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

/**
 * Audio source manager which implements finding audio files from HTTP addresses.
 */
class HttpItemSourceManager @JvmOverloads constructor(containerRegistry: MediaContainerRegistry? = MediaContainerRegistry.DEFAULT_REGISTRY) :
    ProbingItemSourceManager(containerRegistry!!), HttpConfigurable {
    private val httpInterfaceManager: HttpInterfaceManager = ThreadLocalHttpInterfaceManager(
        HttpClientTools
            .createSharedCookiesHttpBuilder()
            .setRedirectStrategy(NoRedirectsStrategy()),
        HttpClientTools.DEFAULT_REQUEST_CONFIG
    )

    override val sourceName: String
        get() = "http"

    /**
     * @return Get an HTTP interface for a playing track.
     */
    val httpInterface: HttpInterface
        get() = httpInterfaceManager.get()

    override suspend fun loadItem(state: LoaderState, reference: AudioReference): AudioItem? {
        val httpReference = getAsHttpReference(reference)
            ?: return null

        return if (httpReference.containerDescriptor != null) {
            createTrack(create(reference, null).build(), httpReference.containerDescriptor)
        } else {
            handleLoadResult(detectContainer(httpReference))
        }
    }

    override fun createTrack(trackInfo: AudioTrackInfo, containerDescriptor: MediaContainerDescriptor): AudioTrack {
        return HttpAudioTrack(trackInfo, containerDescriptor, this)
    }

    override fun configureRequests(configurator: RequestConfigurator) {
        httpInterfaceManager.configureRequests(configurator)
    }

    override fun configureBuilder(configurator: BuilderConfigurator) {
        httpInterfaceManager.configureBuilder(configurator)
    }

    override fun isTrackEncodable(track: AudioTrack): Boolean {
        return true
    }

    @Throws(IOException::class)
    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
        encodeTrackFactory((track as HttpAudioTrack).containerTrackFactory, output)
    }

    @Throws(IOException::class)
    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack? {
        val containerTrackFactory = decodeTrackFactory(input)
        return if (containerTrackFactory != null) {
            HttpAudioTrack(trackInfo, containerTrackFactory, this)
        } else {
            null
        }
    }

    override fun shutdown() {
        // Nothing to shut down
    }

    private fun detectContainer(reference: AudioReference): MediaContainerDetectionResult? {
        var result: MediaContainerDetectionResult?
        try {
            httpInterface.use { httpInterface -> result = detectContainerWithClient(httpInterface, reference) }
        } catch (e: IOException) {
            throw FriendlyException("Connecting to the URL failed.", FriendlyException.Severity.SUSPICIOUS, e)
        }

        return result
    }

    @Throws(IOException::class)
    private fun detectContainerWithClient(
        httpInterface: HttpInterface,
        reference: AudioReference
    ): MediaContainerDetectionResult? {
        try {
            PersistentHttpStream(
                httpInterface,
                URI(reference.identifier),
                Units.CONTENT_LENGTH_UNKNOWN
            ).use { inputStream ->
                val statusCode = inputStream.checkStatusCode()
                val redirectUrl = HttpClientTools.getRedirectLocation(reference.identifier, inputStream.currentResponse)
                when {
                    redirectUrl != null -> return MediaContainerDetectionResult.refer(
                        null,
                        AudioReference(redirectUrl, null)
                    )
                    statusCode == HttpStatus.SC_NOT_FOUND -> return null
                    !HttpClientTools.isSuccessWithContent(statusCode) -> throw FriendlyException(
                        "That URL is not playable.",
                        FriendlyException.Severity.COMMON, IllegalStateException("Status code $statusCode")
                    )
                }

                val hints = from(HttpClientTools.getHeaderValue(inputStream.currentResponse, "Content-Type"), null)
                return MediaContainerDetection(containerRegistry, reference, inputStream, hints).detectContainer()
            }
        } catch (e: URISyntaxException) {
            throw FriendlyException("Not a valid URL.", FriendlyException.Severity.COMMON, e)
        }
    }

    companion object {
        @JvmStatic
        fun getAsHttpReference(reference: AudioReference): AudioReference? {
            if (reference.uri!!.startsWith("https://") || reference.identifier!!.startsWith("http://")) {
                return reference
            } else if (reference.uri!!.startsWith("icy://")) {
                return AudioReference("http://" + reference.identifier.substring(6), reference.title)
            }

            return null
        }
    }
}
