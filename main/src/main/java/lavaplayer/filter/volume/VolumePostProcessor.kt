package lavaplayer.filter.volume

import lavaplayer.filter.AudioPostProcessor
import lavaplayer.track.playback.AudioProcessingContext
import java.nio.ShortBuffer

/**
 * Audio chunk post processor to apply selected volume.
 */
class VolumePostProcessor(private val context: AudioProcessingContext) : AudioPostProcessor {
    private val volumeProcessor: PcmVolumeProcessor = PcmVolumeProcessor(context.playerOptions.volumeLevel.get())

    @Throws(InterruptedException::class)
    override fun process(timecode: Long, buffer: ShortBuffer?) {
        val currentVolume = context.playerOptions.volumeLevel.get()
        if (currentVolume != volumeProcessor.lastVolume) {
            AudioFrameVolumeChanger.apply(context)
        }

        // Volume 0 is stored in the frame with volume 100 buffer
        if (currentVolume != 0) {
            volumeProcessor.applyVolume(100, currentVolume, buffer)
        } else {
            volumeProcessor.lastVolume = 0
        }
    }

    override fun close() {
        // Nothing to close here
    }
}
