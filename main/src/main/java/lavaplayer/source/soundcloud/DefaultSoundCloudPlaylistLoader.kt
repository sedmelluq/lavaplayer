package lavaplayer.source.soundcloud

import lavaplayer.source.common.TrackCollectionLoader
import lavaplayer.tools.FriendlyException
import lavaplayer.tools.extensions.decodeJson
import lavaplayer.tools.io.HttpClientTools
import lavaplayer.tools.io.HttpInterface
import lavaplayer.tools.io.HttpInterfaceManager
import lavaplayer.track.*
import mu.KotlinLogging
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

class DefaultSoundCloudPlaylistLoader(
    private val htmlDataLoader: SoundCloudHtmlDataLoader,
    private val formatHandler: SoundCloudFormatHandler
) : TrackCollectionLoader {
    companion object {
        private const val BASE_API_URL = "https://api-v2.soundcloud.com"
        private val log = KotlinLogging.logger { }
    }

    override fun load(
        identifier: String,
        httpInterfaceManager: HttpInterfaceManager,
        trackFactory: AudioTrackFactory
    ): AudioTrackCollection? {
        try {
            httpInterfaceManager.get().use { httpInterface ->
                val rootData = htmlDataLoader.load(httpInterface, identifier)
                    ?: return null

                val playlist = rootData.resources.firstNotNullOfOrNull { it.data as? SoundCloudPlaylistModel }
                    ?: return null

                return BasicAudioTrackCollection(
                    playlist.title,
                    AudioTrackCollectionType.Playlist,
                    loadPlaylistTracks(httpInterface, playlist, trackFactory),
                )
            }
        } catch (e: IOException) {
            throw FriendlyException(
                "Loading playlist from SoundCloud failed.",
                FriendlyException.Severity.SUSPICIOUS,
                e
            )
        }
    }

    @Throws(IOException::class)
    private fun loadPlaylistTracks(
        httpInterface: HttpInterface,
        playlist: SoundCloudPlaylistModel,
        trackFactory: AudioTrackFactory
    ): MutableList<AudioTrack> {
        val trackIds = playlist.tracks.map { it.id }
        val trackDataList = mutableListOf<SoundCloudTrackModel>()
        for (i in trackIds.indices step 50) {
            val tracks = trackIds.subList(i, (i + 50).coerceAtMost(trackIds.size))
            httpInterface.execute(HttpGet(buildTrackListUrl(tracks))).use { response ->
                HttpClientTools.assertSuccessWithContent(response, "track list response")
                val trackList = response.entity.content.decodeJson<List<SoundCloudTrackModel>>()
                trackDataList.addAll(trackList)
            }
        }

        sortPlaylistTracks(trackDataList, trackIds)

        val tracks = trackDataList
            .filterNot { it.isBlocked }
            .mapNotNull { data ->
                data.runCatching { loadPlaylistTrack(this, trackFactory) }
                    .onFailure { log.error("In soundcloud playlist ${playlist.id}, failed to load track", it) }
                    .getOrNull()
            }

        if (tracks.size < trackDataList.size) {
            val blocked = trackDataList.size - tracks.size
            log.debug { "In soundcloud playlist ${playlist.id}, $blocked tracks were omitted because they are blocked." }
        }

        return tracks.toMutableList()
    }

    private fun loadPlaylistTrack(track: SoundCloudTrackModel, trackFactory: AudioTrackFactory): AudioTrack {
        val format = formatHandler.chooseBestFormat(track.trackFormats)
        val identifier = formatHandler.buildFormatIdentifier(format)
        return trackFactory.create(track.getTrackInfo(identifier))
    }

    private fun buildTrackListUrl(trackIds: List<String>): URI {
        return try {
            URIBuilder("$BASE_API_URL/tracks")
                .addParameter("ids", trackIds.joinToString(","))
                .build()
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }

    private fun sortPlaylistTracks(trackDataList: MutableList<SoundCloudTrackModel>, trackIds: List<String>) {
        val positions = trackIds.indices.associateBy { trackIds[it] }
        trackDataList.sortWith(Comparator.comparingInt { positions[it.id] ?: Int.MAX_VALUE })
    }
}
