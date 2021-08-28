package lavaplayer.track

import lavaplayer.container.MediaContainerDescriptor
import lavaplayer.track.info.AudioTrackInfoProvider

/**
 * An audio item which refers to an unloaded audio item. Source managers can return this to indicate a redirection,
 * which means that the item referred to in it is loaded instead.
 *
 * @param identifier The identifier of the other item.
 * @param title      The title of the other item, if known.
 */
class AudioReference @JvmOverloads constructor(
    /**
     * The identifier of the other item.
     */
    @JvmField override val identifier: String?,
    /**
     * The title of the other item, if known.
     */
    @JvmField override val title: String?,
    /**
     * Known probe and probe settings of the item to be loaded.
     */
    @JvmField val containerDescriptor: MediaContainerDescriptor? = null
) : AudioItem, AudioTrackInfoProvider {
    companion object {
        @JvmField
        val NO_TRACK = AudioReference(null, null, null)
    }

    override val author: String?
        get() = null

    override val length: Long?
        get() = null

    override val artworkUrl: String?
        get() = null

    override val uri: String?
        get() = identifier
}
