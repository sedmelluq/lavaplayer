package lavaplayer.source.soundcloud

class DefaultSoundCloudFormatHandler : SoundCloudFormatHandler {
    companion object {
        private val TYPES = FormatType.values()

        private fun findFormat(formats: List<SoundCloudTrackFormat>, type: FormatType) =
            formats.find { type.matches(it) }

    }

    override fun chooseBestFormat(formats: List<SoundCloudTrackFormat>): SoundCloudTrackFormat {
        // TODO: clean this shit up bro
        return formats.find { format -> TYPES.any { it.matches(format) } }
            ?: throw RuntimeException("Did not detect any supported formats")
    }

    override fun buildFormatIdentifier(format: SoundCloudTrackFormat): String {
        val type = TYPES.find { it.matches(format) }
        return "${"X:".takeIf { type == null } ?: type!!.prefix}${format.lookupUrl}"
    }

    override fun getM3uInfo(identifier: String): SoundCloudM3uInfo? {
        if (identifier.startsWith(FormatType.TYPE_M3U_OPUS.prefix)) {
            return SoundCloudM3uInfo(identifier.substring(2), ::SoundCloudOpusSegmentDecoder)
        } else if (identifier.startsWith(FormatType.TYPE_M3U_MP3.prefix)) {
            return SoundCloudM3uInfo(identifier.substring(2), ::SoundCloudMp3SegmentDecoder)
        }

        return null
    }

    override fun getMp3LookupUrl(identifier: String): String? {
        if (identifier.startsWith("M:")) {
            return identifier.substring(2)
        }

        return null
    }

    enum class FormatType(val protocol: String, val mimeType: String, val prefix: String) {
        TYPE_M3U_OPUS("hls", "audio/ogg", "O:"),
        TYPE_M3U_MP3("hls", "audio/mpeg", "U:"),
        TYPE_SIMPLE_MP3("progressive", "audio/mpeg", "M:");

        fun matches(format: SoundCloudTrackFormat) =
            protocol == format.protocol && format.mimeType.contains(mimeType)
    }
}
