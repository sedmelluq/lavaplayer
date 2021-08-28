import lavaplayer.manager.AudioPlayerManager
import lavaplayer.manager.DefaultAudioPlayerManager
import lavaplayer.source.SourceRegistry
import lavaplayer.tools.FriendlyException
import lavaplayer.tools.extensions.isSearchResult
import lavaplayer.tools.extensions.loadItem
import lavaplayer.track.AudioTrack
import lavaplayer.track.AudioTrackCollection
import lavaplayer.track.loader.ItemLoadResultAdapter
import java.util.concurrent.ExecutionException

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        val playerManager: AudioPlayerManager = DefaultAudioPlayerManager()
        SourceRegistry.registerRemoteSources(playerManager)

        val query = "ytsearch:hey hi hyd glaive"

        /* new item loading */
//        val itemLoaderFactory = DefaultItemLoaderFactory(playerManager)
//        val result = itemLoaderFactory
//            .createItemLoader(query)
//            .load()
//            .get()

        /* old item loading. */
        try {
            playerManager.loadItem(query, object : ItemLoadResultAdapter() {
                override fun onTrackLoad(track: AudioTrack) {
                    println("""
                         ----------------------------------------
                              class:    ${track.javaClass.name}
                              title:    ${track.info.title}
                              author:   ${track.info.author}
                              uri:      ${track.info.uri}
                              duration: ${track.duration}
                              artwork:  ${track.info.artworkUrl}
                        """.trimIndent())
                }

                override fun onCollectionLoad(collection: AudioTrackCollection) {
                    println("search result? ${if (collection.isSearchResult) "yes" else "no"}")
                    for (track in collection.tracks) {
                        println("""
                         ----------------------------------------
                              title:    ${track.info.title}
                              author:   ${track.info.author}
                              uri:      ${track.info.uri}
                              duration: ${track.duration}
                              artwork:  ${track.info.artworkUrl}
                        """.trimIndent())
                    }
                }

                override fun noMatches() {
                    println("No matching items found")
                }

                override fun onLoadFailed(exception: FriendlyException) {
                    exception.printStackTrace()
                }
            }).get()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        }
    }
}
