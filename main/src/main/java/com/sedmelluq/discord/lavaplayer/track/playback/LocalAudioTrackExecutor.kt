package com.sedmelluq.discord.lavaplayer.track.playback

import com.sedmelluq.discord.lavaplayer.manager.AudioConfiguration
import com.sedmelluq.discord.lavaplayer.manager.AudioPlayerResources
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.*
import com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler.MarkerState
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import mu.KotlinLogging
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference

/**
 * Handles the execution and output buffering of an audio track.
 *
 * @param audioTrack      The audio track that this executor executes
 * @param configuration   Configuration to use for audio processing
 * @param playerOptions   Mutable player options (for example volume).
 * @param useSeekGhosting Whether to keep providing old frames continuing from the previous position during a seek
 * until frames from the new position arrive.
 * @param bufferDuration  The size of the frame buffer in milliseconds
 */
class LocalAudioTrackExecutor(
    private val audioTrack: InternalAudioTrack,
    configuration: AudioConfiguration,
    playerOptions: AudioPlayerResources?,
    useSeekGhosting: Boolean,
    bufferDuration: Int
) : AudioTrackExecutor {
    companion object {
        private val log = KotlinLogging.logger {  }
    }

    private var queuedStop by atomic(false)
    private var queuedSeek by atomic(-1L)
    private var lastFrameTimecode by atomic(0L)

    private val useSeekGhosting: Boolean
    private val playingThread = AtomicReference<Thread?>()
    private val actionSynchronizer = Any()
    private val markerTracker = TrackMarkerManager()
    private var externalSeekPosition: Long = -1
    private var interruptableForSeek = false
    @Volatile
    private var trackException: Throwable? = null
    private val isPerformingSeek: Boolean
        get() = queuedSeek != -1L || useSeekGhosting && audioBuffer.hasClearOnInsert()

    override val audioBuffer: AudioFrameBuffer
    override var state by atomic(AudioTrackState.INACTIVE)
    override var position: Long
        get() {
            val seek = queuedSeek
            return if (seek != -1L) seek else lastFrameTimecode
        }
        set(timecode) {
            var timecode = timecode
            if (!audioTrack.isSeekable) {
                return
            }

            synchronized(actionSynchronizer) {
                if (timecode < 0) {
                    timecode = 0
                }

                queuedSeek = timecode
                if (!useSeekGhosting) {
                    audioBuffer.clear()
                }

                interruptForSeek()
            }
        }

    val processingContext: AudioProcessingContext
    val stackTrace: Array<StackTraceElement>?
        get() {
            val thread = playingThread.get()
            if (thread != null) {
                val trace = thread.stackTrace
                if (playingThread.get() === thread) {
                    return trace
                }
            }

            return null
        }

    init {
        val currentFormat = configuration.outputFormat
        audioBuffer = configuration.frameBufferFactory.create(bufferDuration, currentFormat) { queuedStop }
        processingContext = AudioProcessingContext(configuration, audioBuffer, playerOptions!!, currentFormat)
        this.useSeekGhosting = useSeekGhosting
    }

    override fun execute(listener: TrackStateListener?) {
        var interrupt: InterruptedException? = null
        if (Thread.interrupted()) {
            log.debug { "Cleared a stray interrupt." }
        }

        if (playingThread.compareAndSet(null, Thread.currentThread())) {
            log.debug { "Starting to play track ${audioTrack.info.identifier} locally with listener $listener" }
            state = AudioTrackState.LOADING

            try {
                audioTrack.process(this)
                log.debug { "Playing track ${audioTrack.identifier} finished or was stopped." }
            } catch (e: Throwable) {
                // Temporarily clear the interrupted status, so it would not disrupt listener methods.
                interrupt = findInterrupt(e)
                if (interrupt != null && checkStopped()) {
                    log.debug { "Track ${audioTrack.identifier} was interrupted outside of execution loop." }
                } else {
                    audioBuffer.setTerminateOnEmpty()
                    val exception = ExceptionTools.wrapUnfriendlyException("Something broke when playing the track.", FriendlyException.Severity.FAULT, e)
                    ExceptionTools.log(log, exception, "playback of ${audioTrack.identifier}")
                    trackException = exception
                    listener!!.onTrackException(audioTrack, exception)
                    ExceptionTools.rethrowErrors(e)
                }
            } finally {
                synchronized(actionSynchronizer) {
                    interrupt = if (interrupt != null) interrupt else findInterrupt(null)
                    playingThread.compareAndSet(Thread.currentThread(), null)
                    markerTracker.trigger(MarkerState.ENDED)
                    state = AudioTrackState.FINISHED
                }

                if (interrupt != null) {
                    Thread.currentThread().interrupt()
                }
            }
        } else {
            log.warn { "Tried to start an already playing track ${audioTrack.identifier}" }
        }
    }

    override fun stop() {
        synchronized(actionSynchronizer) {
            val thread = playingThread.get()
            if (thread != null) {
                log.debug { "Requesting stop for track ${audioTrack.identifier}" }
                queuedStop = if (!queuedStop) true else queuedStop
                thread.interrupt()
            } else {
                log.debug { "Tried to stop track ${audioTrack.identifier} which is not playing." }
            }
        }
    }

    /**
     * @return True if the track has been scheduled to stop and then clears the scheduled stop bit.
     */
    fun checkStopped(): Boolean {
        if (queuedStop) {
            queuedStop = false
            state = AudioTrackState.STOPPING
            return true
        }

        return false
    }

    /**
     * Wait until all the frames from the frame buffer have been consumed. Keeps the buffering thread alive to keep it
     * interruptable for seeking until buffer is empty.
     *
     * @throws InterruptedException When interrupted externally (or for seek/stop).
     */
    @Throws(InterruptedException::class)
    fun waitOnEnd() {
        audioBuffer.setTerminateOnEmpty()
        audioBuffer.waitForTermination()
    }

    /**
     * Interrupt the buffering thread, either stop or seek should have been set beforehand.
     *
     * @return True if there was a thread to interrupt.
     */
    fun interrupt(): Boolean {
        synchronized(actionSynchronizer) {
            val thread = playingThread.get()
            if (thread != null) {
                thread.interrupt()
                return true
            }

            return false
        }
    }

    override fun setMarker(marker: TrackMarker?) {
        markerTracker[marker] = position
    }

    override fun failedBeforeLoad(): Boolean {
        return trackException != null && !audioBuffer.hasReceivedFrames()
    }

    /**
     * Execute the read and seek loop for the track.
     *
     * @param readExecutor Callback for reading the track
     * @param seekExecutor Callback for performing a seek on the track, may be null on a non-seekable track
     */
    @JvmOverloads
    fun executeProcessingLoop(readExecutor: ReadExecutor, seekExecutor: SeekExecutor?, waitOnEnd: Boolean = true) {
        var proceed = true
        if (checkPendingSeek(seekExecutor) == SeekResult.EXTERNAL_SEEK) {
            return
        }

        while (proceed) {
            state = AudioTrackState.PLAYING
            proceed = false

            try {
                // An interrupt may have been placed while we were handling the previous one.
                if (Thread.interrupted() && !handlePlaybackInterrupt(null, seekExecutor)) {
                    break
                }
                setInterruptableForSeek(true)
                readExecutor.performRead()
                setInterruptableForSeek(false)
                if (seekExecutor != null && externalSeekPosition != -1L) {
                    val nextPosition = externalSeekPosition
                    externalSeekPosition = -1
                    performSeek(seekExecutor, nextPosition)
                    proceed = true
                } else if (waitOnEnd) {
                    waitOnEnd()
                }
            } catch (e: Exception) {
                setInterruptableForSeek(false)
                val interruption = findInterrupt(e)
                proceed = interruption?.let { handlePlaybackInterrupt(it, seekExecutor) }
                    ?: throw ExceptionTools.wrapUnfriendlyException(
                        "Something went wrong when decoding the track.",
                        FriendlyException.Severity.FAULT,
                        e
                    )
            }
        }
    }

    private fun setInterruptableForSeek(state: Boolean) {
        synchronized(actionSynchronizer) { interruptableForSeek = state }
    }

    private fun interruptForSeek() {
        var interrupted = false
        synchronized(actionSynchronizer) {
            if (interruptableForSeek) {
                interruptableForSeek = false
                val thread = playingThread.get()
                if (thread != null) {
                    thread.interrupt()
                    interrupted = true
                }
            }
        }
        if (interrupted) {
            log.debug { "Interrupting playing thread to perform a seek ${audioTrack.identifier}" }
        } else {
            log.debug { "Seeking on track ${audioTrack.identifier} while not in playback loop." }
        }
    }

    private fun handlePlaybackInterrupt(interruption: InterruptedException?, seekExecutor: SeekExecutor?): Boolean {
        Thread.interrupted()
        if (checkStopped()) {
            markerTracker.trigger(MarkerState.STOPPED)
            return false
        }
        val seekResult = checkPendingSeek(seekExecutor)
        return if (seekResult != SeekResult.NO_SEEK) {
            // Double-check, might have received a stop request while seeking
            if (checkStopped()) {
                markerTracker.trigger(MarkerState.STOPPED)
                false
            } else {
                seekResult == SeekResult.INTERNAL_SEEK
            }
        } else if (interruption != null) {
            Thread.currentThread().interrupt()
            throw FriendlyException(
                "The track was unexpectedly terminated.",
                FriendlyException.Severity.SUSPICIOUS,
                interruption
            )
        } else {
            true
        }
    }

    private fun findInterrupt(throwable: Throwable?): InterruptedException? {
        var exception = ExceptionTools.findDeepException(throwable, InterruptedException::class.java)
        if (exception == null) {
            val ioException = ExceptionTools.findDeepException(throwable, InterruptedIOException::class.java)
            if (ioException != null && (ioException.message == null || !ioException.message!!.contains("timed out"))) {
                exception = InterruptedException(ioException.message)
            }
        }
        return if (exception == null && Thread.interrupted()) {
            InterruptedException()
        } else exception
    }

    /**
     * Performs a seek if it scheduled.
     *
     * @param seekExecutor Callback for performing a seek on the track
     * @return True if a seek was performed
     */
    private fun checkPendingSeek(seekExecutor: SeekExecutor?): SeekResult {
        if (!audioTrack.isSeekable) {
            return SeekResult.NO_SEEK
        }

        var seekPosition: Long
        synchronized(actionSynchronizer) {
            seekPosition = queuedSeek
            if (seekPosition == -1L) {
                return SeekResult.NO_SEEK
            }

            log.debug("Track {} interrupted for seeking to {}.", audioTrack.identifier, seekPosition)
            applySeekState(seekPosition)
        }

        return if (seekExecutor != null) {
            performSeek(seekExecutor, seekPosition)
            SeekResult.INTERNAL_SEEK
        } else {
            externalSeekPosition = seekPosition
            SeekResult.EXTERNAL_SEEK
        }
    }

    private fun performSeek(seekExecutor: SeekExecutor, seekPosition: Long) {
        try {
            seekExecutor.performSeek(seekPosition)
        } catch (e: Exception) {
            throw ExceptionTools.wrapUnfriendlyException(
                "Something went wrong when seeking to a position.",
                FriendlyException.Severity.FAULT,
                e
            )
        }
    }

    private fun applySeekState(seekPosition: Long) {
        state = AudioTrackState.SEEKING
        if (useSeekGhosting) {
            audioBuffer.setClearOnInsert()
        } else {
            audioBuffer.clear()
        }

        queuedSeek = -1
        markerTracker.checkSeekTimecode(seekPosition)
    }

    override fun provide(): AudioFrame? {
        val frame = audioBuffer.provide()
        processProvidedFrame(frame)
        return frame
    }

    @Throws(TimeoutException::class, InterruptedException::class)
    override fun provide(timeout: Long, unit: TimeUnit): AudioFrame? {
        val frame = audioBuffer.provide(timeout, unit)
        processProvidedFrame(frame)
        return frame
    }

    override fun provide(targetFrame: MutableAudioFrame): Boolean {
        if (audioBuffer.provide(targetFrame)) {
            processProvidedFrame(targetFrame)
            return true
        }
        return false
    }

    @Throws(TimeoutException::class, InterruptedException::class)
    override fun provide(targetFrame: MutableAudioFrame, timeout: Long, unit: TimeUnit): Boolean {
        if (audioBuffer.provide(targetFrame, timeout, unit)) {
            processProvidedFrame(targetFrame)
            return true
        }

        return true
    }

    private fun processProvidedFrame(frame: AudioFrame?) {
        if (frame != null && !frame.isTerminator) {
            if (!isPerformingSeek) {
                markerTracker.checkPlaybackTimecode(frame.timecode)
            }

            lastFrameTimecode = frame.timecode
        }
    }

    private enum class SeekResult {
        NO_SEEK, INTERNAL_SEEK, EXTERNAL_SEEK
    }

    /**
     * Read executor, see method description
     */
    fun interface ReadExecutor {
        /**
         * Reads until interrupted or EOF.
         *
         * @throws InterruptedException When interrupted externally (or for seek/stop).
         */
        @Throws(Exception::class)
        fun performRead()
    }

    /**
     * Seek executor, see method description
     */
    fun interface SeekExecutor {
        /**
         * Perform a seek to the specified position
         *
         * @param position Position in milliseconds
         */
        @Throws(Exception::class)
        fun performSeek(position: Long)
    }
}
