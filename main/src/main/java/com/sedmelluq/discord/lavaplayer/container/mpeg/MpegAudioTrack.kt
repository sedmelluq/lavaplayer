package com.sedmelluq.discord.lavaplayer.container.mpeg

import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.BaseAudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import kotlinx.atomicfu.update
import mu.KotlinLogging

/**
 * Audio track that handles the processing of MP4 format
 *
 * @param trackInfo   Track info
 * @param inputStream Input stream for the MP4 file
 */
open class MpegAudioTrack(trackInfo: AudioTrackInfo, private val inputStream: SeekableInputStream) : BaseAudioTrack(trackInfo) {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    override fun process(executor: LocalAudioTrackExecutor) {
        val file = MpegFileLoader(inputStream)
        file.parseHeaders()

        val trackConsumer = loadAudioTrack(file, executor.processingContext)
        try {
            val fileReader = file.loadReader(trackConsumer)
                ?: throw FriendlyException("Unknown MP4 format.", FriendlyException.Severity.SUSPICIOUS, null)

            accurateDuration = fileReader.duration
            executor.executeProcessingLoop(
                { fileReader.provideFrames() },
                { timecode: Long -> fileReader.seekToTimecode(timecode) }
            )
        } finally {
            trackConsumer.close()
        }
    }

    protected fun loadAudioTrack(file: MpegFileLoader, context: AudioProcessingContext): MpegTrackConsumer {
        var trackConsumer: MpegTrackConsumer? = null
        var success = false
        return try {
            trackConsumer = selectAudioTrack(file.trackList, context)
            if (trackConsumer == null) {
                throw FriendlyException("The audio codec used in the track is not supported.", FriendlyException.Severity.SUSPICIOUS, null)
            } else {
                log.debug { "Starting to play track with codec ${trackConsumer.track.codecName}" }
            }

            trackConsumer.initialise()
            success = true
            trackConsumer
        } catch (e: Exception) {
            throw ExceptionTools.wrapUnfriendlyException("Something went wrong when loading an MP4 format track.", FriendlyException.Severity.FAULT, e)
        } finally {
            if (!success && trackConsumer != null) {
                trackConsumer.close()
            }
        }
    }

    private fun selectAudioTrack(tracks: List<MpegTrackInfo>, context: AudioProcessingContext): MpegTrackConsumer? {
        for (track in tracks) {
            if ("soun" == track.handler && "mp4a" == track.codecName) {
                return MpegAacTrackConsumer(context, track)
            }
        }

        return null
    }
}
