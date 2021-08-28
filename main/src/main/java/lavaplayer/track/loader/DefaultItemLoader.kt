package lavaplayer.track.loader

import kotlinx.coroutines.async
import lavaplayer.source.ProbingItemSourceManager
import lavaplayer.tools.ExceptionTools
import lavaplayer.tools.FriendlyException
import lavaplayer.track.AudioItem
import lavaplayer.track.AudioReference
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackCollection
import lavaplayer.track.loader.message.DefaultItemLoaderMessages
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

open class DefaultItemLoader(reference: AudioReference, private val factory: DefaultItemLoaderFactory) : ItemLoader {
    companion object {
        private const val MAXIMUM_LOAD_REDIRECTS = 5
        private val log = LoggerFactory.getLogger(DefaultItemLoader::class.java)
    }

    override val state = LoaderState(reference, DefaultItemLoaderMessages())

    override var resultHandler: ItemLoadResultHandler? = null

    override fun loadAsync() = factory.async {
        val itemWasLoaded = AtomicBoolean(false)
        try {
            val result = loadReference(itemWasLoaded)
            if (result is ItemLoadResult.NoMatches) {
                log.debug("No matches found for identifier ${state.reference.identifier}")
            }

            resultHandler?.handle(result)
            result
        } catch (t: Throwable) {
            val exception = ExceptionTools.wrapUnfriendlyExceptions(
                "Something went wrong when looking up the track",
                FriendlyException.Severity.FAULT,
                t
            )
            ExceptionTools.log(log, exception, "loading item ${state.reference.identifier}")
            ItemLoadResult.LoadFailed(exception)
        } finally {
            state.messages.shutdown()
        }
    }

    private fun loadReference(itemWasLoaded: AtomicBoolean): ItemLoadResult {
        var currentReference = state.reference
        var redirects = 0
        while (redirects < MAXIMUM_LOAD_REDIRECTS && currentReference.uri != null) {
            val item = loadReference(currentReference, itemWasLoaded)
                ?: return ItemLoadResult.NoMatches

            when (item) {
                is AudioTrack -> return ItemLoadResult.TrackLoaded(item)
                is AudioTrackCollection -> return ItemLoadResult.CollectionLoaded(item)
                is AudioReference -> currentReference = item
            }

            redirects++
        }

        return ItemLoadResult.NoMatches
    }

    private fun loadReference(reference: AudioReference, itemWasLoaded: AtomicBoolean): AudioItem? {
        for (sourceManager in factory.sourceRegistry.sourceManagers) {
            if (reference.containerDescriptor != null && sourceManager !is ProbingItemSourceManager) {
                continue
            }

            val item = sourceManager.loadItem(state, reference)
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
