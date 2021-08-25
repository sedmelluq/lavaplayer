package lavaplayer.track.loading

import lavaplayer.track.AudioReference
import java.util.concurrent.Future

interface ItemLoader {
    /**
     * The audio reference that holds the identifier that a specific source manager should be able
     * to find a track with.
     */
    val reference: AudioReference

    /**
     * The result handler.
     */
    var resultHandler: ItemLoadResultHandler?

    /**
     * Used for communicating between the item loader and source managers.
     */
    val messages: ItemLoaderMessages

    /**
     * Schedules loading a track (collection) with the specified identifier.
     */
    fun load(): Future<ItemLoadResult>

    /**
     * Schedules loading a track or track collection with the specified identifier with an ordering key so that items with the
     * same ordering key are handled sequentially in the order of calls to this method.
     *
     * @param orderingKey Value to use as the key for the ordering channel.
     */
    fun load(orderingKey: Any): Future<ItemLoadResult>
}
