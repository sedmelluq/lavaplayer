package lavaplayer.container

import lavaplayer.container.adts.AdtsContainerProbe
import lavaplayer.container.flac.FlacContainerProbe
import lavaplayer.container.matroska.MatroskaContainerProbe
import lavaplayer.container.mp3.Mp3ContainerProbe
import lavaplayer.container.mpeg.MpegContainerProbe
import lavaplayer.container.mpegts.MpegAdtsContainerProbe
import lavaplayer.container.ogg.OggContainerProbe
import lavaplayer.container.playlists.M3uPlaylistContainerProbe
import lavaplayer.container.playlists.PlainPlaylistContainerProbe
import lavaplayer.container.playlists.PlsPlaylistContainerProbe
import lavaplayer.container.wav.WavContainerProbe

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
