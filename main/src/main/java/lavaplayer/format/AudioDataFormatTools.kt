package lavaplayer.format

import javax.sound.sampled.AudioFormat

/**
 * Tools to deal with audio data formats.
 */
object AudioDataFormatTools {
    /**
     * @param format Audio data format to convert to JDK audio format
     * @return JDK audio format for the specified format.
     */
    @JvmStatic
    fun toAudioFormat(format: AudioDataFormat): AudioFormat {
        return if (format is Pcm16AudioDataFormat) {
            AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                format.sampleRate.toFloat(),
                16,
                format.channelCount,
                format.channelCount * 2,
                format.sampleRate.toFloat(), format.codecName == Pcm16AudioDataFormat.CODEC_NAME_BE
            )
        } else {
            throw IllegalStateException("Only PCM is currently supported.")
        }
    }
}
