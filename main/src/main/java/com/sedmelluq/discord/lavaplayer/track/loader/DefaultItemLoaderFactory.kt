package com.sedmelluq.discord.lavaplayer.track.loader

import kotlinx.coroutines.*
import com.sedmelluq.lava.common.tools.DaemonThreadFactory
import com.sedmelluq.lava.common.tools.ExecutorTools
import com.sedmelluq.discord.lavaplayer.source.common.SourceRegistry
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

open class DefaultItemLoaderFactory(internal val sourceRegistry: SourceRegistry) : ItemLoaderFactory, CoroutineScope {
    companion object {
        private fun createThreadPool() = ThreadPoolExecutor(
            1,
            10,
            30L,
            TimeUnit.SECONDS,
            SynchronousQueue(false),
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
