package lavaplayer.source.soundcloud

data class DefaultSoundCloudTrackFormat(
    override val trackId: String,
    override val protocol: String,
    override val mimeType: String,
    override val lookupUrl: String
) : SoundCloudTrackFormat
