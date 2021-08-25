package lavaplayer.track.loading

import lavaplayer.tools.FriendlyException
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackCollection

open class ItemLoadResultAdapter : ItemLoadResultHandler {
    /**
     * Called when there were no items found by the specified identifier.
     */
    open fun onNoMatches() {}

    /**
     * Called when loading an item failed with an exception.
     *
     * @param exception The exception that was thrown
     */
    open fun onLoadFailed(exception: FriendlyException) {}

    /**
     * Called when the requested item is a track, and was successfully loaded.
     *
     * @param track The loaded track
     */
    open fun onTrack(track: AudioTrack) {}

    /**
     * Called when the requested item is a track collection and was successfully loaded.
     *
     * @param collection The loaded collection.
     */
    open fun onTrackCollection(trackCollection: AudioTrackCollection) {}

    override suspend fun handle(result: ItemLoadResult) {
        when (result) {
            is ItemLoadResult.NoMatches -> onNoMatches()
            is ItemLoadResult.LoadFailed -> onLoadFailed(result.exception)
            is ItemLoadResult.TrackLoaded -> onTrack(result.track)
            is ItemLoadResult.TrackCollectionLoaded -> onTrackCollection(result.trackCollection)
            else -> throw error("Unknown item load result type: ${result::class.qualifiedName}")
        }
    }
}
