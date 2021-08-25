package lavaplayer.manager;

import lavaplayer.format.AudioDataFormat;
import lavaplayer.format.StandardAudioDataFormats;
import lavaplayer.track.playback.AllocatingAudioFrameBuffer;
import lavaplayer.track.playback.AudioFrameBufferFactory;
import lavaplayer.track.playback.factory
import kotlin.math.max

/**
 * Configuration for audio processing.
 */
class AudioConfiguration {
    companion object {
        const val OPUS_QUALITY_MAX = 10;
    }

    var resamplingQuality: ResamplingQuality = ResamplingQuality.LOW;
    var outputFormat: AudioDataFormat = StandardAudioDataFormats.DISCORD_OPUS;
    @get:JvmName("isFilterHotSwapEnabled")
    var filterHotSwapEnabled: Boolean = false;
    var frameBufferFactory: AudioFrameBufferFactory = AllocatingAudioFrameBuffer::class.factory()
    var opusEncodingQuality: Int = OPUS_QUALITY_MAX
        set(value) {
            field = max(0, minOf(value, OPUS_QUALITY_MAX));
        }

    /**
     * @return A copy of this configuration.
     */
    fun copy(): AudioConfiguration {
        val copy = AudioConfiguration();
        copy.resamplingQuality = resamplingQuality
        copy.opusEncodingQuality = opusEncodingQuality
        copy.outputFormat = outputFormat
        copy.filterHotSwapEnabled = filterHotSwapEnabled;
        copy.frameBufferFactory = frameBufferFactory;
        return copy;
    }

    /**
     * Resampling quality levels
     */
    enum class ResamplingQuality {
        HIGH,
        MEDIUM,
        LOW
    }
}
