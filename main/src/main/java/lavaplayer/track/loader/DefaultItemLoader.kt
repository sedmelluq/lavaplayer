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
import mu.KotlinLogging

open class DefaultItemLoader(reference: AudioReference, private val factory: DefaultItemLoaderFactory) : ItemLoader {
    companion object {
        private const val MAXIMUM_LOAD_REDIRECTS = 5
        private val log = KotlinLogging.logger { }
    }

    override val state = LoaderState(reference, DefaultItemLoaderMessages())

    override var resultHandler: ItemLoadResultHandler? = null

    override suspend fun load(): ItemLoadResult {
        return try {
            val result = loadReference()
            if (result is ItemLoadResult.NoMatches) {
                log.debug { "No matches found for identifier '${state.reference.identifier}'" }
            }

            resultHandler?.handle(result)
            result
        } catch (t: Throwable) {
            val exception = ExceptionTools.wrapUnfriendlyException(
                "Something went wrong when looking up the track",
                FriendlyException.Severity.FAULT,
                t
            )
            ExceptionTools.log(log, exception, "loading item '${state.reference.identifier}'")
            ItemLoadResult.LoadFailed(exception)
        } finally {
            state.messages.shutdown()
        }
    }

    override fun loadAsync() = factory.async {
        load()
    }

    private suspend fun loadReference(): ItemLoadResult {
        var currentReference = state.reference
        for (redirect in 0 until MAXIMUM_LOAD_REDIRECTS) {
            if (currentReference.identifier == null) {
                break
            }

            val item = loadReference(currentReference)
                ?: return ItemLoadResult.NoMatches

            when (item) {
                is AudioTrack -> return ItemLoadResult.TrackLoaded(item)
                is AudioTrackCollection -> return ItemLoadResult.CollectionLoaded(item)
                is AudioReference -> currentReference = item
            }
        }

        return ItemLoadResult.NoMatches
    }

    private suspend fun loadReference(reference: AudioReference): AudioItem? {
        for (sourceManager in factory.sourceRegistry.sourceManagers) {
            if (reference.containerDescriptor != null && sourceManager !is ProbingItemSourceManager) {
                continue
            }

            val item = sourceManager.loadItem(state, reference)
                ?: continue

            if (item !is AudioReference) {
                val name = "audio track${" collection".takeIf { item is AudioTrackCollection } ?: ""}"
                log.debug { "Loaded an $name with identifier '${reference.uri}' using ${sourceManager::class.qualifiedName}." }
            }

            return item
        }

        return null
    }
}
