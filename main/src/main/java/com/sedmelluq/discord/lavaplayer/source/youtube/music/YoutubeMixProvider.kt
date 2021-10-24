package com.sedmelluq.discord.lavaplayer.source.youtube.music

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeConstants
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools.durationTextToMillis
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.ThumbnailTools
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import com.sedmelluq.discord.lavaplayer.tools.json.JsonBrowser
import com.sedmelluq.discord.lavaplayer.track.*
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import java.io.IOException

/**
 * Handles loading of YouTube mixes.
 */
class YoutubeMixProvider : YoutubeMixLoader {
    /**
     * Loads tracks from mix in parallel into a playlist entry.
     *
     * @param mixId ID of the mix
     * @param selectedVideoId Selected track, [AudioTrackCollection.selectedTrack] will return this.
     * @return Playlist of the tracks in the mix.
     */
    override fun load(
        httpInterface: HttpInterface,
        mixId: String,
        selectedVideoId: String?,
        trackFactory: AudioTrackFactory
    ): AudioTrackCollection {
        var playlistTitle = "YouTube mix"
        val tracks: MutableList<AudioTrack> = ArrayList()
        val post = HttpPost(YoutubeConstants.NEXT_URL)
        val payload = StringEntity(java.lang.String.format(YoutubeConstants.NEXT_PAYLOAD, selectedVideoId, mixId), "UTF-8")
        post.entity = payload

        try {
            httpInterface.execute(post).use { response ->
                HttpClientTools.assertSuccessWithContent(response, "mix response")
                val body = JsonBrowser.parse(response.entity.content)
                val playlist = body["contents"]["singleColumnWatchNextResults"]["results"]["results"]["contents"]

                val title = playlist["title"]
                if (!title.isNull) {
                    playlistTitle = title.safeText
                }

                println(body.format())
                extractPlaylistTracks(playlist["contents"], tracks, trackFactory)
            }
        } catch (e: IOException) {
            throw FriendlyException("Could not read mix page.", FriendlyException.Severity.SUSPICIOUS, e)
        }

        if (tracks.isEmpty()) {
            throw FriendlyException("Could not find tracks from mix.", FriendlyException.Severity.SUSPICIOUS, null)
        }

        val selectedTrack = findSelectedTrack(tracks, selectedVideoId)
        return BasicAudioTrackCollection(playlistTitle, AudioTrackCollectionType.Playlist, tracks, selectedTrack)
    }

    private fun extractPlaylistTracks(
        browser: JsonBrowser,
        tracks: MutableList<AudioTrack>,
        trackFactory: AudioTrackFactory
    ) {
        for (renderer in browser.values().map { it["playlistPanelVideoRenderer"] }) {
            val videoId = renderer["videoId"].text!!

            /* create the audio track. */
            val trackInfo = AudioTrackInfo(
                title = renderer["title"]["runs"][0]["text"].text!!,
                author = renderer["longBylineText"]["runs"][0]["text"].text!!,
                length = durationTextToMillis(renderer["lengthText"]["runs"][0]["text"].text!!),
                identifier = videoId,
                uri = "https://youtube.com/watch?v=$videoId",
                artworkUrl = ThumbnailTools.extractYouTube(renderer, videoId),
            )

            tracks.add(trackFactory.create(trackInfo))
        }
    }

    private fun findSelectedTrack(tracks: List<AudioTrack>, selectedVideoId: String?): AudioTrack? {
        if (selectedVideoId != null) {
            return tracks.find { it.identifier == selectedVideoId }
        }

        return null
    }
}

