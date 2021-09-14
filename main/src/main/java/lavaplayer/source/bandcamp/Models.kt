package lavaplayer.source.bandcamp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BandcampTrackListModel(
    val artist: String,
    @SerialName("trackinfo")
    val trackInfo: List<BandcampTrackModel>,
    @SerialName("art_id")
    val artId: String,
    val current: BandcampResourceModel
) {
    companion object {
        private const val ARTWORK_URL_FORMAT = "https://f4.bcbits.com/img/a%s_9.jpg"
    }

    val normalizedArtId: String
        get() = artId.takeUnless { it.length < 10 }
            ?: artId.padStart(10 - artId.length, '0')

    val artworkUrl
        get() = ARTWORK_URL_FORMAT.format(normalizedArtId)
}

@Serializable
data class BandcampResourceModel(val title: String)

@Serializable
data class BandcampTrackModel(
    @SerialName("title_link")
    val titleLink: String,
    val duration: Double,
    val title: String,
    val id: String,
    val file: File
) {
    @Serializable
    data class File(@SerialName("mp3-128") val url: String)
}
