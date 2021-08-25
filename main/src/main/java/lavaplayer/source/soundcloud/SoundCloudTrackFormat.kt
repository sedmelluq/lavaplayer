package lavaplayer.source.soundcloud

interface SoundCloudTrackFormat {
    val trackId: String

    val protocol: String

    val mimeType: String

    val lookupUrl: String
}
