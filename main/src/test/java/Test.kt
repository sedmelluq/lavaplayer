import kotlinx.coroutines.runBlocking
import lavaplayer.manager.AudioPlayerManager
import lavaplayer.manager.DefaultAudioPlayerManager
import lavaplayer.source.SourceRegistry
import lavaplayer.tools.extensions.isSearchResult
import lavaplayer.track.loader.ItemLoadResult
import java.util.concurrent.ExecutionException

object Test {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val playerManager: AudioPlayerManager = DefaultAudioPlayerManager()
        SourceRegistry.registerRemoteSources(playerManager)

        val query = "ytsearch:hey hi hyd glaive"
        try {
            when (val result = playerManager.items.createItemLoader(query).load()) {
                is ItemLoadResult.TrackLoaded -> println(
                    """
                         ----------------------------------------
                              class:    ${result.track.javaClass.name}
                              title:    ${result.track.info.title}
                              author:   ${result.track.info.author}
                              uri:      ${result.track.info.uri}
                              duration: ${result.track.duration}
                              artwork:  ${result.track.info.artworkUrl}
                        """.trimIndent()
                )

                is ItemLoadResult.CollectionLoaded -> {
                    println("search result? ${if (result.collection.isSearchResult) "yes" else "no"}")
                    for (track in result.collection.tracks) {
                        println(
                            """
                         ----------------------------------------
                              title:    ${track.info.title}
                              author:   ${track.info.author}
                              uri:      ${track.info.uri}
                              duration: ${track.duration}
                              artwork:  ${track.info.artworkUrl}
                        """.trimIndent()
                        )
                    }
                }

                is ItemLoadResult.NoMatches ->
                    println("No matching items found")

                is ItemLoadResult.LoadFailed ->
                    result.exception.printStackTrace()
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        }
    }
}
