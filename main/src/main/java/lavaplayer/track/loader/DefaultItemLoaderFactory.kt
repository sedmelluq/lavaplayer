package lavaplayer.track.loader

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import lavaplayer.common.tools.DaemonThreadFactory
import lavaplayer.common.tools.ExecutorTools
import lavaplayer.source.SourceRegistry
import lavaplayer.track.AudioReference
import kotlin.coroutines.CoroutineContext

class DefaultItemLoaderFactory(internal val sourceRegistry: SourceRegistry) : ItemLoaderFactory, CoroutineScope {
    companion object {
        private fun createThreadPool() = ExecutorTools.createEagerlyScalingExecutor(
            1,
            10,
            30L * 60000L,
            5000,
            DaemonThreadFactory("lp item-loader")
        )
    }

    private val loaderThreadPool = createThreadPool()
    private val loaderDispatcher = loaderThreadPool.asCoroutineDispatcher()

    override val coroutineContext: CoroutineContext
        get() = loaderDispatcher + SupervisorJob() + CoroutineName("Item Loader Factory")

    override var itemLoaderPoolSize: Int
        get() = loaderThreadPool.poolSize
        set(value) {
            loaderThreadPool.maximumPoolSize = value
        }

    override fun createItemLoader(reference: AudioReference): ItemLoader {
        return DefaultItemLoader(reference, this)
    }

    override fun shutdown() {
        ExecutorTools.shutdownExecutor(loaderThreadPool, "track info")
    }
}
