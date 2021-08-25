package lavaplayer.source.soundcloud

import lavaplayer.tools.JsonBrowser
import lavaplayer.track.AudioTrackInfo

interface SoundCloudDataReader {
    fun findTrackData(rootData: JsonBrowser): JsonBrowser

    fun readTrackId(trackData: JsonBrowser): String

    fun isTrackBlocked(trackData: JsonBrowser): Boolean

    fun readTrackInfo(trackData: JsonBrowser, identifier: String): AudioTrackInfo

    fun readTrackFormats(trackData: JsonBrowser): List<SoundCloudTrackFormat>

    fun findPlaylistData(rootData: JsonBrowser): JsonBrowser

    fun readPlaylistName(playlistData: JsonBrowser): String

    fun readPlaylistIdentifier(playlistData: JsonBrowser): String

    fun readPlaylistTracks(playlistData: JsonBrowser): List<JsonBrowser>
}
