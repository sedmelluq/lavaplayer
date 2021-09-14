package lavaplayer.source.soundcloud

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import lavaplayer.track.AudioTrackInfo

@Serializable(with = SoundCloudResource.Companion::class)
sealed class SoundCloudResource {
    @Serializable
    data class Unknown(val value: JsonElement) : SoundCloudResource()

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    companion object : KSerializer<SoundCloudResource> {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("SoundCloudResource", PolymorphicKind.SEALED)

        @Suppress("UNCHECKED_CAST")
        override fun serialize(encoder: Encoder, value: SoundCloudResource) {
            if (value is Unknown) {
                (value.value::class.serializer() as KSerializer<JsonElement>).serialize(encoder, value.value)
                return
            }

            (value::class.serializer() as KSerializer<SoundCloudResource>).serialize(encoder, value)
        }

        override fun deserialize(decoder: Decoder): SoundCloudResource {
            val jsonDecoder = decoder as JsonDecoder
            val jsonElement = jsonDecoder.decodeJsonElement()
            return if (jsonElement is JsonObject) {
                when (jsonElement["kind"]?.jsonPrimitive?.content) {
                    "playlist" -> jsonDecoder.json.decodeFromJsonElement<SoundCloudPlaylistModel>(jsonElement)
                    "track" -> jsonDecoder.json.decodeFromJsonElement<SoundCloudTrackModel>(jsonElement)
                    else -> Unknown(jsonElement)
                }
            } else {
                Unknown(jsonElement)
            }
        }
    }
}

@Serializable
@JvmInline
value class SoundCloudRootDataModel(val resources: List<HydratedObject>) {
    @Serializable
    data class HydratedObject(val hydratable: String, val data: SoundCloudResource)
}

@Serializable
enum class SoundCloudResourceKind {
    @SerialName("playlist")
    Playlist,

    @SerialName("track")
    Track,
    Unknown;
}

@Serializable
enum class SoundCloudTrackPolicy {
    @SerialName("ALLOW")
    Allow,

    @SerialName("BLOCK")
    Block,

    @SerialName("MONETIZE")
    Monetize,
    Unknown;
}

@Serializable
data class SoundCloudUserLikedModel(val collection: List<Entry>) {
    @Serializable
    data class Entry(val track: SoundCloudTrackModel)
}

@Serializable
data class SoundCloudSearchResultModel(val collection: List<SoundCloudTrackModel>)

@Serializable
data class SoundCloudPlaylistModel(
    val id: String,
    val title: String,
    val permalink: String,
    @SerialName("is_album")
    val isAlbum: Boolean,
    val tracks: List<SoundCloudTrackModel>
) : SoundCloudResource()

@Serializable
data class SoundCloudTrackModel(
    val id: String,
    val title: String = "",
    val user: SoundCloudUserModel? = null,
    @SerialName("permalink_url")
    val permalinkUrl: String = "",
    @SerialName("artwork_url")
    val artworkUrl: String? = "",
    val duration: Long = -1L,
    val media: Media = Media(emptyList()),
    val policy: SoundCloudTrackPolicy = SoundCloudTrackPolicy.Unknown
) : SoundCloudResource() {
    val trackFormats: List<SoundCloudTrackFormat>
        get() = media.transcodings.map {
            DefaultSoundCloudTrackFormat(id, it.format.protocol, it.format.mimeType, it.url)
        }

    val isBlocked: Boolean
        get() = policy == SoundCloudTrackPolicy.Block

    fun getTrackInfo(identifier: String) = AudioTrackInfo(
        title = title,
        author = user?.username ?: "Unknown Artist",
        length = duration,
        uri = permalinkUrl,
        artworkUrl = artworkUrl ?: user?.avatarUrl?.replace("large.jpg", "original.jpg"),
        identifier = identifier
    )

    @Serializable
    data class Media(val transcodings: List<SoundCloudTrackTranscodingModel>)
}

@Serializable
data class SoundCloudUserModel(val username: String, @SerialName("avatar_url") val avatarUrl: String)

@Serializable
data class SoundCloudTrackTranscodingModel(val url: String, val format: Format) {
    @Serializable
    data class Format(@SerialName("mime_type") val mimeType: String, val protocol: String)
}
