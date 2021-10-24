package com.sedmelluq.discord.lavaplayer.container

import com.sedmelluq.discord.lavaplayer.container.adts.AdtsContainerProbe
import com.sedmelluq.discord.lavaplayer.container.flac.FlacContainerProbe
import com.sedmelluq.discord.lavaplayer.container.matroska.MatroskaContainerProbe
import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3ContainerProbe
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegContainerProbe
import com.sedmelluq.discord.lavaplayer.container.mpegts.MpegAdtsContainerProbe
import com.sedmelluq.discord.lavaplayer.container.ogg.OggContainerProbe
import com.sedmelluq.discord.lavaplayer.container.playlists.M3uPlaylistContainerProbe
import com.sedmelluq.discord.lavaplayer.container.playlists.PlainPlaylistContainerProbe
import com.sedmelluq.discord.lavaplayer.container.playlists.PlsPlaylistContainerProbe
import com.sedmelluq.discord.lavaplayer.container.wav.WavContainerProbe

/**
 * Lists currently supported containers and their probes.
 */
enum class MediaContainer(
    /**
     * The probe used to detect files using this container and create the audio tracks for them.
     */
    val probe: MediaContainerProbe
) {
    WAV(WavContainerProbe()),
    MKV(MatroskaContainerProbe()),
    MP4(MpegContainerProbe()),
    FLAC(FlacContainerProbe()),
    OGG(OggContainerProbe()),
    M3U(M3uPlaylistContainerProbe()),
    PLS(PlsPlaylistContainerProbe()),
    PLAIN(PlainPlaylistContainerProbe()),
    MP3(Mp3ContainerProbe()),
    ADTS(AdtsContainerProbe()),
    MPEGADTS(MpegAdtsContainerProbe());

    companion object {
        fun asList(): MutableList<MediaContainerProbe> {
            return values()
                .map { it.probe }
                .toMutableList()
        }
    }
}
