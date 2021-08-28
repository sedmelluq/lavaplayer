package lavaplayer.format

/**
 * Standard output formats compatible with Discord.
 */
object StandardAudioDataFormats {
    /**
     * The Opus configuration used by both Discord and YouTube. Default.
     */
    @JvmField
    val DISCORD_OPUS: AudioDataFormat = OpusAudioDataFormat(2, 48000, 960)

    /**
     * Signed 16-bit big-endian PCM format matching the parameters used by Discord.
     */
    @JvmField
    val DISCORD_PCM_S16_BE: AudioDataFormat = Pcm16AudioDataFormat(2, 48000, 960, true)

    /**
     * Signed 16-bit little-endian PCM format matching the parameters used by Discord.
     */
    @JvmField
    val DISCORD_PCM_S16_LE: AudioDataFormat = Pcm16AudioDataFormat(2, 48000, 960, false)

    /**
     * Signed 16-bit big-endian PCM format matching with the most common sample rate.
     */
    @JvmField
    val COMMON_PCM_S16_BE: AudioDataFormat = Pcm16AudioDataFormat(2, 44100, 960, true)

    /**
     * Signed 16-bit big-endian PCM format matching with the most common sample rate.
     */
    @JvmField
    val COMMON_PCM_S16_LE: AudioDataFormat = Pcm16AudioDataFormat(2, 44100, 960, false)
}
