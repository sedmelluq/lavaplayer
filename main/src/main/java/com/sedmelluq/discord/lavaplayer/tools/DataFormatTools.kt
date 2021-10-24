package com.sedmelluq.discord.lavaplayer.tools

import org.apache.commons.io.IOUtils
import org.apache.http.NameValuePair
import org.apache.http.client.utils.URLEncodedUtils
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset

typealias TextRange = Pair<String, String>

/**
 * Helper methods related to Strings, Maps, and Numbers.
 */
object DataFormatTools {
    /**
     * Parses a duration string into milliseconds
     *
     * @param duration The string duration
     * @return The duration in milliseconds.
     */
    @JvmStatic
    fun parseDuration(duration: String): Long {
        val parts = duration.split(":".toRegex())
        return when (parts.size) {
            3 -> {
                val hours = parts[0].toInt()
                val minutes = parts[1].toInt()
                val seconds = parts[2].toInt()
                hours * 3600000L + minutes * 60000L + seconds * 1000L
            }
            2 -> {
                val minutes = parts[0].toInt()
                val seconds = parts[1].toInt()
                minutes * 60000L + seconds * 1000L
            }
            else -> Units.DURATION_MS_UNKNOWN
        }
    }

    /**
     * Extract text between the first subsequent occurrences of start and end in haystack
     *
     * @param haystack The text to search from
     * @param start    The text after which to start extracting
     * @param end      The text before which to stop extracting
     * @return The extracted string
     */
    @JvmStatic
    fun extractBetween(haystack: String, start: String, end: String): String? {
        val startMatch = haystack.indexOf(start)
        if (startMatch >= 0) {
            val startPosition = startMatch + start.length
            val endPosition = haystack.indexOf(end, startPosition)
            if (endPosition >= 0) {
                return haystack.substring(startPosition, endPosition)
            }
        }

        return null
    }

    @JvmStatic
    fun extractBetween(haystack: String, candidates: List<TextRange>): String? {
        return candidates.firstNotNullOfOrNull { extractBetween(haystack, it.first, it.second) }
    }

    /**
     * Converts name value pairs to a map, with the last entry for each name being present.
     *
     * @param pairs Name value pairs to convert
     * @return The resulting map
     */
    @JvmStatic
    fun convertToMapLayout(pairs: List<NameValuePair>): Map<String, String> {
        val map: MutableMap<String, String> = mutableMapOf()
        for (pair in pairs) {
            map[pair.name] = pair.value
        }

        return map
    }

    @JvmStatic
    fun decodeUrlEncodedItems(input: String, escapedSeparator: Boolean): Map<String, String> {
        var input = input
        if (escapedSeparator) {
            input = input.replace("""\\u0026""", "&")
        }

        return convertToMapLayout(URLEncodedUtils.parse(input, Charsets.UTF_8))
    }

    /**
     * Returns the specified default value if the value itself is null.
     *
     * @param value        Value to check
     * @param defaultValue Default value to return if value is null
     * @param <T>          The type of the value
     * @return Value or default value
    </T> */
    @JvmStatic
    fun <T> defaultOnNull(value: T?, defaultValue: T): T {
        return value ?: defaultValue
    }

    /**
     * Consumes a stream and returns it as lines.
     *
     * @param inputStream Input stream to consume.
     * @param charset     Character set of the stream
     * @return Lines from the stream
     * @throws IOException On read error
     */
    @JvmStatic
    @Throws(IOException::class)
    fun streamToLines(inputStream: InputStream, charset: Charset?): List<String> {
        return IOUtils.toString(inputStream, charset).lines()
    }

    /**
     * Converts duration in the format HH:mm:ss (or mm:ss or ss) to milliseconds. Does not support day count.
     *
     * @param durationText Duration in text format.
     * @return Duration in milliseconds.
     */
    @JvmStatic
    fun durationTextToMillis(durationText: String): Long {
        var length = 0
        for (part in durationText.split("[:.]".toRegex())) {
            length = length * 60 + part.toInt()
        }

        return length * 1000L
    }

    /**
     * Writes a string to output with the additional information whether it is `null` or not. Compatible with
     * [readNullableText].
     *
     * @param output Output to write to.
     * @param text   Text to write.
     * @throws IOException On write error.
     */
    @Throws(IOException::class)
    fun writeNullableText(output: DataOutput, text: String?) {
        output.writeBoolean(text != null)
        if (text != null) {
            output.writeUTF(text)
        }
    }

    /**
     * Reads a string from input which may be `null`. Compatible with
     * [writeNullableText].
     *
     * @param input Input to read from.
     * @return The string that was read, or `null`.
     * @throws IOException On read error.
     */
    @Throws(IOException::class)
    fun readNullableText(input: DataInput): String? {
        val exists = input.readBoolean()
        return if (exists) input.readUTF() else null
    }

    @JvmStatic
    fun arrayRangeEquals(array: ByteArray, offset: Int, segment: ByteArray): Boolean {
        if (array.size < offset + segment.size) {
            return false
        }

        return segment.withIndex().all { (it, _) -> segment[it] == array[it + offset] }
    }
}
