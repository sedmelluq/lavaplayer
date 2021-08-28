package lavaplayer.source.soundcloud

import lavaplayer.tools.JsonBrowser
import lavaplayer.tools.ThumbnailTools.extractSoundCloud
import lavaplayer.track.AudioTrackInfo
import org.slf4j.LoggerFactory

class DefaultSoundCloudDataReader : SoundCloudDataReader {
    companion object {
        private val log = LoggerFactory.getLogger(DefaultSoundCloudDataReader::class.java)
    }

    override fun findTrackData(rootData: JsonBrowser): JsonBrowser {
        return findEntryOfKind(rootData, "track")!!
    }

    override fun readTrackId(trackData: JsonBrowser): String {
        return trackData["id"].safeText
    }

    override fun isTrackBlocked(trackData: JsonBrowser): Boolean {
        return "BLOCK" == trackData["policy"].safeText
    }

    override fun readTrackInfo(trackData: JsonBrowser, identifier: String): AudioTrackInfo {
        return AudioTrackInfo(
            trackData["title"].safeText,
            trackData["user"]["username"].safeText,
            trackData["duration"].asInt().toLong(),
            identifier,
            false,
            trackData["permalink_url"].text,
            extractSoundCloud(trackData)
        )
    }

    override fun readTrackFormats(trackData: JsonBrowser): List<SoundCloudTrackFormat> {
        val formats = mutableListOf<SoundCloudTrackFormat>()
        val trackId = readTrackId(trackData)
        if (trackId.isEmpty()) {
            log.warn("Track data {} missing track ID: {}.", trackId, trackData.format())
        }

        for (transcoding in trackData["media"]["transcodings"].values()) {
            val format = transcoding["format"]
            val protocol = format["protocol"].safeText
            val mimeType = format["mime_type"].safeText
            if (protocol.isNotEmpty() && mimeType.isNotEmpty()) {
                val lookupUrl = transcoding["url"].safeText
                if (lookupUrl.isNotEmpty()) {
                    formats.add(DefaultSoundCloudTrackFormat(trackId, protocol, mimeType, lookupUrl))
                } else {
                    log.warn("Transcoding of {} missing url: {}.", trackId, transcoding.format())
                }
            } else {
                log.warn("Transcoding of {} missing protocol/mimetype: {}.", trackId, transcoding.format())
            }
        }

        return formats
    }

    override fun findPlaylistData(rootData: JsonBrowser): JsonBrowser {
        return findEntryOfKind(rootData, "playlist")!!
    }

    override fun readPlaylistName(playlistData: JsonBrowser): String {
        return playlistData["title"].safeText
    }

    override fun readPlaylistIdentifier(playlistData: JsonBrowser): String {
        return playlistData["permalink"].safeText
    }

    override fun readPlaylistTracks(playlistData: JsonBrowser): List<JsonBrowser> {
        return playlistData["tracks"].values()
    }

    private fun findEntryOfKind(data: JsonBrowser, kind: String): JsonBrowser? {
        for (value in data.values()) {
            if (value.isMap && kind == value["data"]["kind"].safeText) {
                return value["data"]
            }
        }

        return null
    }
}
