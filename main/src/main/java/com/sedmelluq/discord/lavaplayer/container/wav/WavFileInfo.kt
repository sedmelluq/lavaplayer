package com.sedmelluq.discord.lavaplayer.container.wav

/**
 * WAV file format information.
 *
 * @param channelCount  Number of channels.
 * @param sampleRate    Sample rate.
 * @param bitsPerSample Bits per sample (currently only 16 supported).
 * @param blockAlign    Size of a block (one sample for each channel + padding).
 * @param blockCount    Number of blocks in the file.
 * @param startOffset   Starting position of the raw PCM samples in the file.
 */
data class WavFileInfo(
    /**
     * Number of channels.
     */
    @JvmField val channelCount: Int,
    /**
     * Sample rate.
     */
    @JvmField val sampleRate: Int,
    /**
     * Bits per sample (currently only 16 supported).
     */
    @JvmField val bitsPerSample: Int,
    /**
     * Size of a block (one sample for each channel + padding).
     */
    @JvmField val blockAlign: Int,
    /**
     * Number of blocks in the file.
     */
    @JvmField val blockCount: Long,
    /**
     * Starting position of the raw PCM samples in the file.
     */
    @JvmField val startOffset: Long
) {
    /**
     * @return Duration of the file in milliseconds.
     */
    val duration: Long
        get() = blockCount * 1000L / sampleRate

    /**
     * @return The size of padding in a sample block in bytes.
     */
    val padding: Int
        get() = blockAlign - channelCount * (bitsPerSample shr 3)
}
