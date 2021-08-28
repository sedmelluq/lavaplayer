package lavaplayer.track

import kotlinx.serialization.Serializable
import lavaplayer.tools.DataFormatTools
import lavaplayer.tools.io.MessageInput
import java.io.DataInput
import java.io.DataOutput
import kotlin.experimental.and

/**
 * Meta info for an audio track
 *
 * @param title      Track title
 * @param author     Track author, if known
 * @param length     Length of the track in milliseconds
 * @param identifier Audio source specific track identifier
 * @param isStream   True if this track is a stream
 * @param uri        URL of the track or path to its file.
 */
@Serializable
data class AudioTrackInfo(
    /**
     * Track title
     */
    @JvmField
    val title: String,
    /**
     * Track author, if known
     */
    @JvmField
    val author: String,
    /**
     * Length of the track in milliseconds, UnitConstants.DURATION_MS_UNKNOWN for streams
     */
    @JvmField
    val length: Long,
    /**
     * Audio source specific track identifier
     */
    @JvmField
    val identifier: String,
    /**
     * True if this track is a stream
     */
    @JvmField
    val isStream: Boolean,
    /**
     * URL of the track, or local path to the file.
     */
    @JvmField
    val uri: String?,
    /**
     * URL of the artwork for this track.
     */
    @JvmField
    val artworkUrl: String?
) {
    companion object {
        @JvmStatic
        fun getVersion(stream: MessageInput, input: DataInput): Int =
            if ((stream.messageFlags and TrackEncoder.TRACK_INFO_VERSIONED) != 0) {
                (input.readByte() and 0xFF.toByte()).toInt()
            } else {
                1
            }

        @JvmStatic
        fun encode(output: DataOutput, info: AudioTrackInfo) {
            output.writeUTF(info.title)
            output.writeUTF(info.author)
            output.writeLong(info.length)
            output.writeUTF(info.identifier)
            output.writeBoolean(info.isStream)
            DataFormatTools.writeNullableText(output, info.uri)
            DataFormatTools.writeNullableText(output, info.artworkUrl)
        }

        @JvmStatic
        fun decode(input: DataInput) = AudioTrackInfo(
            input.readUTF(),
            input.readUTF(),
            input.readLong(),
            input.readUTF(),
            input.readBoolean(),
            DataFormatTools.readNullableText(input),
            DataFormatTools.readNullableText(input)
        )
    }
}
