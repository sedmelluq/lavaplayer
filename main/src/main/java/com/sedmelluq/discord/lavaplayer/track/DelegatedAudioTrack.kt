package com.sedmelluq.discord.lavaplayer.track

import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor

/**
 * Audio track which delegates its processing to another track. The delegate does not have to be known when the
 * track is created, but is passed when processDelegate() is called.
 *
 * @param trackInfo Track info
 */
abstract class DelegatedAudioTrack(trackInfo: AudioTrackInfo) : BaseAudioTrack(trackInfo) {
    private var delegate: InternalAudioTrack? = null

    fun processDelegate(delegate: InternalAudioTrack, localExecutor: LocalAudioTrackExecutor) = synchronized(this) {

        this.delegate = delegate

        delegate.assignExecutor(localExecutor, false)
        delegate.process(localExecutor)
    }

    override fun getDuration(): Long = delegate?.duration ?: synchronized(this) {
        delegate?.duration ?: super.getDuration()
    }
}
