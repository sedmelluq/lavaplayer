package lavaplayer.manager;

import lavaplayer.tools.FriendlyException;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackCollection;

import java.util.function.Consumer;

/**
 * Helper class for creating an audio result handler using only methods that can be passed as lambdas.
 *
 * @param trackConsumer      Consumer for single track result
 * @param playlistConsumer   Consumer for playlist result
 * @param emptyResultHandler Empty result handler
 * @param exceptionConsumer  Consumer for an exception when loading the item fails
 */
class FunctionalResultHandler(
    private val trackConsumer: Consumer<AudioTrack>?,
    private val collectionConsumer: Consumer<AudioTrackCollection>?,
    private val emptyResultHandler: Runnable?,
    private val exceptionConsumer: Consumer<FriendlyException>?,
) : AudioLoadResultHandler {
    override fun trackLoaded(track: AudioTrack) {
        trackConsumer?.accept(track)
    }

    override fun collectionLoaded(collection: AudioTrackCollection) {
        collectionConsumer?.accept(collection)
    }

    override fun noMatches() {
        emptyResultHandler?.run()
    }

    override fun loadFailed(exception: FriendlyException) {
        exceptionConsumer?.accept(exception)
    }
}
