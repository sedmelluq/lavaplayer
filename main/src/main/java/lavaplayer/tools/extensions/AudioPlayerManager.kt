package lavaplayer.tools.extensions

import lavaplayer.manager.AudioPlayerManager
import lavaplayer.track.AudioReference
import lavaplayer.track.loading.ItemLoadResult
import lavaplayer.track.loading.ItemLoadResultAdapter
import java.util.concurrent.Future

fun AudioPlayerManager.loadItem(identifier: String, handler: ItemLoadResultAdapter): Future<ItemLoadResult> =
    loadItem(AudioReference(identifier, null), handler)

fun AudioPlayerManager.loadItem(reference: AudioReference, handler: ItemLoadResultAdapter): Future<ItemLoadResult> {
    val itemLoader = itemLoaders.createItemLoader(reference)
    itemLoader.resultHandler = handler
    return itemLoader.load()
}

fun AudioPlayerManager.loadItem(orderingKey: Any, identifier: String, handler: ItemLoadResultAdapter): Future<ItemLoadResult> =
    loadItem(orderingKey, AudioReference(identifier, null), handler)


fun AudioPlayerManager.loadItem(orderingKey: Any, reference: AudioReference, handler: ItemLoadResultAdapter): Future<ItemLoadResult> {
    val itemLoader = itemLoaders.createItemLoader(reference)
    itemLoader.resultHandler = handler
    return itemLoader.load(orderingKey)
}
