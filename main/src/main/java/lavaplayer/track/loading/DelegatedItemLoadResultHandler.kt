package lavaplayer.track.loading

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
    override fun onTrack(track: AudioTrack) {
        trackConsumer?.accept(track)
    }

    override fun onTrackCollection(trackCollection: AudioTrackCollection) {
        collectionConsumer?.accept(trackCollection)
    }

    override fun onNoMatches() {
        emptyResultHandler?.run()
    }

    override fun onLoadFailed(exception: FriendlyException) {
        exceptionConsumer?.accept(exception)
    }
}
