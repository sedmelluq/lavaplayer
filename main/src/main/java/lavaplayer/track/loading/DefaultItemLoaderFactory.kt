package lavaplayer.track.loading

import lavaplayer.common.tools.DaemonThreadFactory
import lavaplayer.common.tools.ExecutorTools
import lavaplayer.source.Sources
import lavaplayer.tools.OrderedExecutor
import lavaplayer.track.AudioReference

class DefaultItemLoaderFactory(internal val sources: Sources) : ItemLoaderFactory {
    companion object {
        private const val DEFAULT_LOADER_POOL_SIZE = 10
        private const val LOADER_QUEUE_CAPACITY = 5000
    }

    internal val trackInfoExecutor = ExecutorTools.createEagerlyScalingExecutor(1, DEFAULT_LOADER_POOL_SIZE, 30L * 60000L, LOADER_QUEUE_CAPACITY, DaemonThreadFactory("lp item-loader"))
    internal val orderedInfoExecutor = OrderedExecutor(trackInfoExecutor)

    override var itemLoaderPoolSize: Int
        get() = trackInfoExecutor.poolSize
        set(value) {
            trackInfoExecutor.maximumPoolSize = value
        }

    override fun createItemLoader(reference: AudioReference): ItemLoader {
        return DefaultItemLoader(reference, this)
    }

    override fun shutdown() {
        ExecutorTools.shutdownExecutor(trackInfoExecutor, "track info");
    }
}
