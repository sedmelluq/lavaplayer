package com.sedmelluq.discord.lavaplayer.source.local

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDescriptor
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import java.io.File

/**
 * Audio track that handles processing local files as audio tracks.
 *
 * @param trackInfo             Track info
 * @param containerTrackFactory Probe track factory - contains the probe with its parameters.
 * @param sourceManager         Source manager used to load this track
 */
class LocalAudioTrack(
    trackInfo: AudioTrackInfo,
    /**
     * The media probe which handles creating a container-specific delegated track for this track.
     */
    val containerTrackFactory: MediaContainerDescriptor,
    override val sourceManager: LocalItemSourceManager
) : DelegatedAudioTrack(trackInfo) {
    private val file = File(trackInfo.identifier)

    @Throws(Exception::class)
    override fun process(executor: LocalAudioTrackExecutor) {
        LocalSeekableInputStream(file).use { inputStream ->
            processDelegate(
                (containerTrackFactory.createTrack(
                    info,
                    inputStream
                ) as InternalAudioTrack), executor
            )
        }
    }

    override fun makeShallowClone(): AudioTrack {
        return LocalAudioTrack(info, containerTrackFactory, sourceManager)
    }
}
