package lavaplayer.track.loading

import lavaplayer.tools.FriendlyException
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackCollection

interface ItemLoadResult {
    /**
     * There were no items found by the specified identifier.
     */
    object NoMatches : ItemLoadResult

    /**
     * The requested item had failed to load.
     */
    data class LoadFailed(val exception: FriendlyException) : ItemLoadResult

    /**
     * The requested item was a successfully loaded audio track.
     */
    data class TrackLoaded(val track: AudioTrack) : ItemLoadResult

    /**
     * The requested item was a collection of tracks.
     */
    data class TrackCollectionLoaded(val trackCollection: AudioTrackCollection) : ItemLoadResult
}
