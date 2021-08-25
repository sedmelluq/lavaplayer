package lavaplayer.track.playback

import lavaplayer.format.AudioDataFormat

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

/**
 * Factory for audio frame buffers.
 */
fun interface AudioFrameBufferFactory {
    /**
     * @param bufferDuration Maximum duration of the buffer. The buffer may actually hold less in case the average size of
     *                       frames exceeds {@link AudioDataFormat#expectedChunkSize()}.
     * @param format         The format of the frames held in this buffer.
     * @param stopping       Atomic boolean which has true value when the track is in a state of pending stop.
     * @return A new frame buffer instance.
     */
    fun create(bufferDuration: Int, format: AudioDataFormat, stopping: AtomicBoolean): AudioFrameBuffer
}

fun KClass<out AudioFrameBuffer>.factory() =
    AudioFrameBufferFactory { b, f, s -> constructors.first().call(b, f, s) }
