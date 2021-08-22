package com.sedmelluq.discord.lavaplayer.tools

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import java.io.IOException
import java.io.InputStream
import kotlin.reflect.KClass

class JsonBrowser(@get:JvmName("element") val element: JsonElement) {

    companion object {
        @JvmField
        val NULL_BROWSER = JsonBrowser(JsonNull)

        val format = Json {
            isLenient = true
        }

        @JvmStatic
        fun create(element: JsonElement): JsonBrowser {
            return if (element is JsonNull) NULL_BROWSER else JsonBrowser(element)
        }

        /**
         * Parse from string.
         * @param json The JSON object as a string
         * @return JsonBrowser instance for navigating in the result
         * @throws IOException When parsing the JSON failed
         */
        @JvmStatic
        @Throws(IOException::class)
        fun parse(json: String) =
            JsonBrowser(format.decodeFromString(json))

        /**
         * Parse from string.
         * @param stream The JSON object as a stream
         * @return JsonBrowser instance for navigating in the result
         * @throws IOException When parsing the JSON failed
         */
        @JvmStatic
        fun parse(stream: InputStream) =
            parse(stream.reader().readText())
    }

    val isNull: Boolean
        get() = element is JsonNull

    val isList: Boolean
        get() = element is JsonArray

    val isMap: Boolean
        get() = element is JsonObject

    @get:JvmName("text")
    val text: String?
        get() {
            if (element !is JsonNull) {
                if (element is JsonPrimitive) {
                    return element.content
                }

                return element.toString()
            }

            return null
        }

    @get:JvmName("safeText")
    val safeText: String
        get() = text ?: ""

    fun index(index: Int) =
        get(index)

    /**
     * Get an element at an index for a list value
     * @param index List index
     * @return JsonBrowser instance which wraps the value at the specified index
     */
    operator fun get(index: Int): JsonBrowser = if (element is JsonArray && index >= 0 && index < element.size) {
        create(element[index])
    } else {
        NULL_BROWSER
    }

    /**
     * Get an element by key from a map value
     * @param key Map key
     * @return JsonBrowser instance which wraps the value with the specified key
     */
    operator fun get(key: String): JsonBrowser = if (element is JsonObject) {
        create(element[key] ?: JsonNull)
    } else {
        NULL_BROWSER
    }

    fun values(): List<JsonBrowser> {
        val values = mutableListOf<JsonElement>()
        when (element) {
            is JsonArray -> values.addAll(element)
            is JsonObject -> values.addAll(element.values)
            else -> values.add(element)
        }

        return values.map { JsonBrowser(it) }
    }

    fun format(): String =
        format.encodeToString(element)

    inline fun <reified T> cast(): T {
        return format.decodeFromJsonElement(element)
    }

    inline fun <reified T> safeCast(): T? {
        return element
            .runCatching { format.decodeFromJsonElement<T>(element) }
            .getOrNull()
    }

    fun asDouble(): Double =
        cast()

    fun asDouble(default: Double): Double =
        safeCast() ?: default

    fun asFloat(): Float =
        cast()

    fun asInt(): Int =
        cast()

    fun asLong(): Long =
        cast()

    fun asLong(default: Long): Long =
        safeCast() ?: default

    fun asBoolean(default: Boolean): Boolean =
        safeCast() ?: default

    fun asBoolean(): Boolean =
        cast()

    override fun equals(other: Any?): Boolean {
        return element.equals(other)
    }

    @OptIn(InternalSerializationApi::class)
    fun <T : Any> cast(kclass: KClass<T>): T {
        return format.decodeFromJsonElement(kclass.serializer(), element)
    }

}
