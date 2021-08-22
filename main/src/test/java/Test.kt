import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackCollection
import com.sedmelluq.discord.lavaplayer.track.isSearchResult
import java.util.concurrent.ExecutionException

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        val playerManager: AudioPlayerManager = DefaultAudioPlayerManager()
        AudioSourceManagers.registerRemoteSources(playerManager)
        val query = "https://music.youtube.com/playlist?list=PLaHN2HRhxJFeq7IgXL5mQnn2pA0VI0oYu"
//        val query = "ytmsearch:resume jenevieve"
        try {
            playerManager.loadItem(query, object : AudioLoadResultHandler {
                override fun trackLoaded(track: AudioTrack) {
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

                override fun collectionLoaded(playlist: AudioTrackCollection) {
                    println("search result? ${if (playlist.isSearchResult) "yes" else "no"}")
                    for (track in playlist.tracks) {
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

                override fun loadFailed(exception: FriendlyException) {
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
