package lavaplayer.track.loading

import kotlinx.coroutines.cancel
import lavaplayer.manager.DefaultAudioPlayerManager
import lavaplayer.source.ProbingItemSourceManager
import lavaplayer.tools.ExceptionTools
import lavaplayer.tools.FriendlyException
import lavaplayer.tools.SuspendedCallable
import lavaplayer.track.AudioItem
import lavaplayer.track.AudioReference
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackCollection
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

open class DefaultItemLoader(override val reference: AudioReference, private val factory: DefaultItemLoaderFactory) : ItemLoader {
    companion object {
        private const val MAXIMUM_LOAD_REDIRECTS = 5
        private val log = LoggerFactory.getLogger(DefaultItemLoader::class.java)
    }

    override var resultHandler: ItemLoadResultHandler? = null

    override val messages = ItemLoaderMessages()

    override fun load(): Future<ItemLoadResult> =
        loadReference(null)

    override fun load(orderingKey: Any): Future<ItemLoadResult> =
        loadReference(orderingKey)

    private fun executorQueueFull(e: Throwable): ItemLoadResult.LoadFailed {
        val exception = FriendlyException("Cannot queue loading a track, queue is full.", FriendlyException.Severity.SUSPICIOUS, e)
        ExceptionTools.log(log, exception, "queueing item ${reference.uri}")
        return ItemLoadResult.LoadFailed(exception)
    }

    private fun loadReference(orderingKey: Any?): Future<ItemLoadResult> = try {
        if (orderingKey != null) {
            factory.orderedInfoExecutor.submit(orderingKey, loadReference().asJavaCallable())
        } else {
            factory.trackInfoExecutor.submit(loadReference().asJavaCallable())
        }
    } catch (e: Throwable) {
        val future = CompletableFuture<ItemLoadResult>()
        future.complete(executorQueueFull(e))
        future
    }

    private fun loadReference() = SuspendedCallable {
        val itemWasLoaded = AtomicBoolean(false)
        try {
            val result = loadReference(itemWasLoaded)
            if (result is ItemLoadResult.NoMatches) {
                log.debug("No matches found for identifier ${reference.uri}")
            }

            resultHandler?.handle(result)
            result
        } catch (t: Throwable) {
            val exception = ExceptionTools.wrapUnfriendlyExceptions("Something went wrong when looking up the track", FriendlyException.Severity.FAULT, t)
            ExceptionTools.log(log, exception, "loading item ${reference.uri}")
            ItemLoadResult.LoadFailed(exception)
        } finally {
            messages.cancel("Item loader has finished")
        }
    }

    private fun loadReference(itemWasLoaded: AtomicBoolean): ItemLoadResult {
        var currentReference = reference
        var redirects = 0
        while (redirects < MAXIMUM_LOAD_REDIRECTS && currentReference.uri != null) {
            val item = loadReference(currentReference, itemWasLoaded)
                ?: return ItemLoadResult.NoMatches

            when (item) {
                is AudioTrack -> return ItemLoadResult.TrackLoaded(item)
                is AudioTrackCollection -> return ItemLoadResult.TrackCollectionLoaded(item)
                is AudioReference -> currentReference = item
            }

            redirects++
        }

        return ItemLoadResult.NoMatches
    }

    private fun loadReference(reference: AudioReference, itemWasLoaded: AtomicBoolean): AudioItem? {
        for (sourceManager in factory.sources.sourceManagers) {
            if (reference.containerDescriptor != null && sourceManager !is ProbingItemSourceManager) {
                continue
            }

            val item = sourceManager.loadItem(this, reference)
                ?: continue

            if (item !is AudioReference) {
                val name = "audio track${" collection".takeIf { item is AudioTrackCollection } ?: ""}"
                log.debug("Loaded an $name with identifier ${reference.uri} using ${sourceManager::class.qualifiedName}.")
                itemWasLoaded.set(true)
            }

            return item
        }

        return null
    }
}
