package com.sedmelluq.discord.lavaplayer.source.local

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDescriptor
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints.Companion.from
import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry
import com.sedmelluq.discord.lavaplayer.source.ProbingItemSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.loader.LoaderState
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
        val file = File(reference.identifier!!)
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
}
