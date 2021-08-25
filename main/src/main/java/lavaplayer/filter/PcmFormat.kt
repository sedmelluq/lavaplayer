package lavaplayer.filter

/**
 * Describes the properties of PCM data.
 *
 * @param channelCount See [.channelCount].
 * @param sampleRate   See [.sampleRate].
 */
class PcmFormat(
    /**
     * Number of channels.
     */
    @JvmField val channelCount: Int,
    /**
     * Sample rate (frequency).
     */
    @JvmField val sampleRate: Int
)
