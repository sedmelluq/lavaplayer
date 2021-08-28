package lavaplayer.tools.extensions

import kotlinx.coroutines.Deferred
import lavaplayer.manager.AudioPlayerManager
import lavaplayer.track.AudioReference
import lavaplayer.track.loader.ItemLoadResult
import lavaplayer.track.loader.ItemLoadResultAdapter

fun AudioPlayerManager.loadItemAsync(identifier: String, handler: ItemLoadResultAdapter): Deferred<ItemLoadResult> =
    loadItemAsync(AudioReference(identifier, null), handler)

fun AudioPlayerManager.loadItemAsync(reference: AudioReference, handler: ItemLoadResultAdapter): Deferred<ItemLoadResult> {
    val itemLoader = itemLoaders.createItemLoader(reference)
    itemLoader.resultHandler = handler
    return itemLoader.loadAsync()
}
