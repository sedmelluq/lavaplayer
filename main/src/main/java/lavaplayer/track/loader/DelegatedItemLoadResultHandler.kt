package lavaplayer.track.loader

import lavaplayer.tools.FriendlyException
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackCollection
import java.util.function.Consumer

class DelegatedItemLoadResultHandler(
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
