package lavaplayer.source.common

import lavaplayer.tools.io.HttpInterfaceManager
import lavaplayer.track.AudioTrackCollection
import lavaplayer.track.AudioTrackFactory

interface TrackCollectionLoader {
    /**
     * Loads a new audio track collection with the provided [identifier]
     *
     * @param identifier The identifier to use
     * @param httpInterfaceManager
     * @param trackFactory
     */
    fun load(
        identifier: String,
        httpInterfaceManager: HttpInterfaceManager,
        trackFactory: AudioTrackFactory
    ): AudioTrackCollection?
}
