package com.sedmelluq.discord.lavaplayer.tools.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import java.io.InputStream

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
object JsonTools {
    val format = Json {
        isLenient = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    inline fun <reified T : Any> decode(stream: InputStream): T =
        format.decodeFromStream(T::class.serializer(), stream)

    inline fun <reified T : Any> decode(json: String): T =
        format.decodeFromString(T::class.serializer(), json)

    inline fun <reified T : Any> encode(thing: T): String =
        format.encodeToString(T::class.serializer(), thing)
}
