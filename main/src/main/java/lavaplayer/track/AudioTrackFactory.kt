package lavaplayer.track

fun interface AudioTrackFactory {
    /**
     * Creates a new [AudioTrack] from the provided [AudioTrackInfo]
     *
     * @param info The info to create the new [AudioTrack] from.
     *
     * @return a new [AudioTrack]
     */
    fun create(info: AudioTrackInfo): AudioTrack
}
