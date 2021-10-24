package com.sedmelluq.discord.lavaplayer.source

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDescriptor
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult
import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

/**
 * The base class for audio sources which use probing to detect container type.
 */
abstract class ProbingItemSourceManager(@JvmField protected val containerRegistry: MediaContainerRegistry) :
    ItemSourceManager {
    companion object {
        private const val PARAMETERS_SEPARATOR = '|'
    }

    protected abstract fun createTrack(
        trackInfo: AudioTrackInfo,
        containerDescriptor: MediaContainerDescriptor
    ): AudioTrack

    protected fun handleLoadResult(result: MediaContainerDetectionResult?): AudioItem? = if (result != null) {
        when {
            result.isReference -> result.reference
            !result.isContainerDetected -> throw FriendlyException(
                "Unknown file format.",
                FriendlyException.Severity.COMMON,
                null
            )
            !result.isSupportedFile -> throw FriendlyException(
                result.unsupportedReason,
                FriendlyException.Severity.COMMON,
                null
            )
            else -> createTrack(result.trackInfo, result.containerDescriptor)
        }
    } else {
        null
    }

    @Throws(IOException::class)
    protected fun encodeTrackFactory(factory: MediaContainerDescriptor, output: DataOutput) {
        val probeInfo = factory.probe.name + if (factory.parameters != null) {
            PARAMETERS_SEPARATOR.toString() + factory.parameters
        } else {
            ""
        }

        output.writeUTF(probeInfo)
    }

    @Throws(IOException::class)
    protected fun decodeTrackFactory(input: DataInput): MediaContainerDescriptor? {
        val probeInfo = input.readUTF()
        val separatorPosition = probeInfo.indexOf(PARAMETERS_SEPARATOR)
        val probeName = if (separatorPosition < 0) probeInfo else probeInfo.substring(0, separatorPosition)
        val parameters = if (separatorPosition < 0) null else probeInfo.substring(separatorPosition + 1)
        val probe = containerRegistry.find(probeName)

        return probe?.let { MediaContainerDescriptor(it, parameters) }
    }
}
