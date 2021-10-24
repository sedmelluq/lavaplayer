package com.sedmelluq.discord.lavaplayer.track.loader

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackCollection
import java.util.function.Consumer

open class DelegatedItemLoadResultHandler(
    var trackConsumer: Consumer<AudioTrack>?,
    var collectionConsumer: Consumer<AudioTrackCollection>?,
    var emptyResultHandler: Runnable?,
    var exceptionConsumer: Consumer<FriendlyException>?,
) : ItemLoadResultAdapter() {
    override fun onTrackLoad(track: AudioTrack) {
        trackConsumer?.accept(track)
    }

    override fun onCollectionLoad(collection: AudioTrackCollection) {
        collectionConsumer?.accept(collection)
    }

    override fun noMatches() {
        emptyResultHandler?.run()
    }

    override fun onLoadFailed(exception: FriendlyException) {
        exceptionConsumer?.accept(exception)
    }
}
