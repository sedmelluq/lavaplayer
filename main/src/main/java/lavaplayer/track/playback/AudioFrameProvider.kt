package lavaplayer.track.playback

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * A provider for audio frames
 */
interface AudioFrameProvider {
    /**
     * @return Provided frame, or null if none available
     */
    fun provide(): AudioFrame?

    /**
     * @param timeout Specifies the maximum time to wait for data. Pass 0 for non-blocking mode.
     * @param unit    Specifies the time unit of the maximum wait time.
     * @return Provided frame. In case wait time is above zero, null indicates that no data is not available at the
     * current moment, otherwise null means the end of the track.
     * @throws TimeoutException     When wait time is above zero, but no track info is found in that time.
     * @throws InterruptedException When interrupted externally (or for seek/stop).
     */
    @Throws(TimeoutException::class, InterruptedException::class)
    fun provide(timeout: Long, unit: TimeUnit): AudioFrame?

    /**
     * @param targetFrame Frame to update with the details and data of the provided frame.
     * @return `true` if a frame was provided.
     */
    fun provide(targetFrame: MutableAudioFrame): Boolean

    /**
     * @param targetFrame Frame to update with the details and data of the provided frame.
     * @param timeout     Timeout.
     * @param unit        Time unit for the timeout value.
     * @return `true` if a frame was provided.
     * @throws TimeoutException     If no frame became available within the timeout.
     * @throws InterruptedException When interrupted externally (or for seek/stop).
     */
    @Throws(TimeoutException::class, InterruptedException::class)
    fun provide(targetFrame: MutableAudioFrame, timeout: Long, unit: TimeUnit): Boolean
}
