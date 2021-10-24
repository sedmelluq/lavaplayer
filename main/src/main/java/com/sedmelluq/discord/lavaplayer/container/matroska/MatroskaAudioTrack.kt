package com.sedmelluq.discord.lavaplayer.container.matroska

import com.sedmelluq.discord.lavaplayer.container.matroska.format.MatroskaFileTrack
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.BaseAudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import kotlinx.atomicfu.update
import mu.KotlinLogging
import java.io.IOException

/**
 * Audio track that handles the processing of MKV and WEBM formats
 *
 * @param trackInfo   Track info
 * @param inputStream Input stream for the file
 */
class MatroskaAudioTrack(trackInfo: AudioTrackInfo, private val inputStream: SeekableInputStream) : BaseAudioTrack(trackInfo) {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    override fun process(executor: LocalAudioTrackExecutor) {
        val file = loadMatroskaFile()
        val trackConsumer = loadAudioTrack(file, executor.processingContext)
        try {
            executor.executeProcessingLoop(
                { file.provideFrames(trackConsumer) },
                { file.seekToTimecode(trackConsumer!!.track.index, it) }
            )
        } finally {
            ExceptionTools.closeWithWarnings(trackConsumer)
        }
    }

    private fun loadMatroskaFile(): MatroskaStreamingFile {
        return try {
            val file = MatroskaStreamingFile(inputStream)
            file.readFile()
            accurateDuration = file.duration.toLong()
            file
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun loadAudioTrack(file: MatroskaStreamingFile, context: AudioProcessingContext): MatroskaTrackConsumer? {
        var trackConsumer: MatroskaTrackConsumer? = null
        var success = false

        try {
            trackConsumer = selectAudioTrack(file.trackList, context)
            checkNotNull(trackConsumer) { "No supported audio tracks in the file." }
            log.debug("Starting to play track with codec {}", trackConsumer.track.codecId)
            trackConsumer.initialise()
            success = true
        } finally {
            if (!success && trackConsumer != null) {
                ExceptionTools.closeWithWarnings(trackConsumer)
            }
        }

        return trackConsumer
    }

    private fun selectAudioTrack(tracks: Array<MatroskaFileTrack>, context: AudioProcessingContext): MatroskaTrackConsumer? {
        var trackConsumer: MatroskaTrackConsumer? = null
        for (track in tracks) {
            if (track.type == MatroskaFileTrack.Type.AUDIO) {
                if (MatroskaContainerProbe.OPUS_CODEC == track.codecId) {
                    trackConsumer = MatroskaOpusTrackConsumer(context, track)
                    break
                } else if (MatroskaContainerProbe.VORBIS_CODEC == track.codecId) {
                    trackConsumer = MatroskaVorbisTrackConsumer(context, track)
                } else if (MatroskaContainerProbe.AAC_CODEC == track.codecId) {
                    trackConsumer = MatroskaAacTrackConsumer(context, track)
                }
            }
        }

        return trackConsumer
    }
}
