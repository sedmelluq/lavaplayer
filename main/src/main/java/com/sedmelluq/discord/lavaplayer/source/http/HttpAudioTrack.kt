package com.sedmelluq.discord.lavaplayer.source.http

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDescriptor
import com.sedmelluq.discord.lavaplayer.tools.Units
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import mu.KotlinLogging
import java.net.URI

/**
 * Audio track that handles processing HTTP addresses as audio tracks.
 *
 * @param trackInfo             Track info
 * @param containerTrackFactory Container track factory - contains the probe with its parameters.
 * @param sourceManager         Source manager used to load this track
 */
class HttpAudioTrack(
    trackInfo: AudioTrackInfo,
    /**
     * The media probe which handles creating a container-specific delegated track for this track.
     */
    val containerTrackFactory: MediaContainerDescriptor,
    override val sourceManager: HttpItemSourceManager
) : DelegatedAudioTrack(trackInfo) {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    override fun process(executor: LocalAudioTrackExecutor) {
        sourceManager.httpInterface.use { httpInterface ->
            log.debug { "Starting http track from URL: ${info.identifier}" }
            PersistentHttpStream(httpInterface, URI(info.identifier), Units.CONTENT_LENGTH_UNKNOWN).use { stream ->
                processDelegate(containerTrackFactory.createTrack(info, stream) as InternalAudioTrack, executor)
            }
        }
    }

    override fun makeShallowClone() =
        HttpAudioTrack(info, containerTrackFactory, sourceManager)
}
