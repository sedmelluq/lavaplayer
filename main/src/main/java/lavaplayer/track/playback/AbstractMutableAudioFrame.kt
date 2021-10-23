package lavaplayer.track.playback

import lavaplayer.format.AudioDataFormat


/**
 * Base class for mutable audio frames.
 */
abstract class AbstractMutableAudioFrame : AudioFrame {
    override var timecode: Long = 0

    override var volume: Int = 0

    override var isTerminator: Boolean = false

    override var format: AudioDataFormat? = null

    /**
     * @return An immutable instance created from this mutable audio frame. In an ideal flow, this should never be called.
     */
    fun freeze(): ImmutableAudioFrame {
        return ImmutableAudioFrame(timecode, data, volume, format!!)
    }
}
