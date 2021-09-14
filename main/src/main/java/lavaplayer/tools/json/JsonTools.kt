package lavaplayer.tools.json

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.InputStream

@OptIn(InternalSerializationApi::class)
object JsonTools {
    val format = Json {
        isLenient = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun browse(json: String): JsonBrowser =
        JsonBrowser.parse(json)

    fun browse(stream: InputStream): JsonBrowser =
        JsonBrowser.parse(stream)

    inline fun <reified T : Any> decode(json: String): T =
        format.decodeFromString(T::class.serializer(), json)
}
