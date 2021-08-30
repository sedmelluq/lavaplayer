package lavaplayer.source.local

import lavaplayer.container.MediaContainerDescriptor
import lavaplayer.container.MediaContainerDetection
import lavaplayer.container.MediaContainerDetectionResult
import lavaplayer.container.MediaContainerHints.Companion.from
import lavaplayer.container.MediaContainerRegistry
import lavaplayer.source.ProbingItemSourceManager
import lavaplayer.tools.FriendlyException
import lavaplayer.track.AudioItem
import lavaplayer.track.AudioReference
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackInfo
import lavaplayer.track.loader.LoaderState
import java.io.DataInput
import java.io.DataOutput
import java.io.File
import java.io.IOException

/**
 * Audio source manager that implements finding audio files from the local file system.
 */
class LocalItemSourceManager @JvmOverloads constructor(containerRegistry: MediaContainerRegistry? = MediaContainerRegistry.DEFAULT_REGISTRY) :
    ProbingItemSourceManager(containerRegistry!!) {
    override val sourceName: String
        get() = "local"

    override suspend fun loadItem(state: LoaderState, reference: AudioReference): AudioItem? {
        val file = File(reference.identifier)
        return if (file.exists() && file.isFile && file.canRead()) {
            handleLoadResult(detectContainerForFile(reference, file))
        } else {
            null
        }
    }

    override fun createTrack(trackInfo: AudioTrackInfo, containerDescriptor: MediaContainerDescriptor): AudioTrack {
        return LocalAudioTrack(trackInfo, containerDescriptor, this)
    }

    private fun detectContainerForFile(reference: AudioReference, file: File): MediaContainerDetectionResult {
        try {
            LocalSeekableInputStream(file).use { inputStream ->
                val lastDotIndex = file.name.lastIndexOf('.')
                val fileExtension = if (lastDotIndex >= 0) file.name.substring(lastDotIndex + 1) else null
                return MediaContainerDetection(containerRegistry, reference, inputStream, from(null, fileExtension))
                    .detectContainer()
            }
        } catch (e: IOException) {
            throw FriendlyException("Failed to open file for reading.", FriendlyException.Severity.SUSPICIOUS, e)
        }
    }

    override fun isTrackEncodable(track: AudioTrack): Boolean {
        return true
    }

    @Throws(IOException::class)
    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
        encodeTrackFactory((track as LocalAudioTrack).containerTrackFactory, output)
    }

    @Throws(IOException::class)
    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack? {
        val containerTrackFactory = decodeTrackFactory(input)
        return if (containerTrackFactory != null) LocalAudioTrack(trackInfo, containerTrackFactory, this) else null
    }

    override fun shutdown() {
        // Nothing to shut down
    }
}
