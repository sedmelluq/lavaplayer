package lavaplayer.track

import lavaplayer.track.playback.AudioFrameProvider
import lavaplayer.track.playback.AudioTrackExecutor
import kotlin.Throws
import lavaplayer.track.playback.LocalAudioTrackExecutor
import lavaplayer.manager.AudioPlayerManager
import java.lang.Exception

/**
 * Methods of an audio track that should not be visible outside the library
 */
interface InternalAudioTrack : AudioTrack, AudioFrameProvider {
    /**
     * @return Get the active track executor
     */
    val activeExecutor: AudioTrackExecutor

    /**
     * @param executor             Executor to assign to the track
     * @param applyPrimordialState True if the state previously applied to this track should be copied to new executor.
     */
    fun assignExecutor(executor: AudioTrackExecutor?, applyPrimordialState: Boolean)

    /**
     * Perform any necessary loading and then enter the read/seek loop
     *
     * @param executor The local executor which processes this track
     * @throws Exception In case anything explodes.
     */
    @Throws(Exception::class)
    fun process(executor: LocalAudioTrackExecutor)

    /**
     * @param playerManager The player manager which is executing this track
     * @return A custom local executor for this track. Unless this track requires a special executor, this should return
     * null as the default one will be used in that case.
     */
    fun createLocalExecutor(playerManager: AudioPlayerManager?): AudioTrackExecutor?
}
