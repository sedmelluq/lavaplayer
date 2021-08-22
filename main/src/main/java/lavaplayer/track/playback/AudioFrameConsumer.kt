package lavaplayer.track.playback

/**
 * A consumer for audio frames
 */
interface AudioFrameConsumer {
    /**
     * Consumes the frame, may block
     *
     * @param frame The frame to consume
     * @throws InterruptedException When interrupted externally (or for seek/stop).
     */
    @Throws(InterruptedException::class)
    fun consume(frame: AudioFrame)

    /**
     * Rebuild all caches frames
     *
     * @param rebuilder The rebuilder to use
     */
    fun rebuild(rebuilder: AudioFrameRebuilder)
}
