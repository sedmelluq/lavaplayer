package com.sedmelluq.discord.lavaplayer.track.playback;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerOptions;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import com.sedmelluq.discord.lavaplayer.track.TrackMarkerTracker;
import com.sedmelluq.discord.lavaplayer.track.TrackStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InterruptedIOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.sedmelluq.discord.lavaplayer.tools.ExceptionTools.findDeepException;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.FAULT;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;
import static com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler.MarkerState.ENDED;
import static com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler.MarkerState.STOPPED;

/**
 * Handles the execution and output buffering of an audio track.
 */
public class LocalAudioTrackExecutor implements AudioTrackExecutor {
  private static final Logger log = LoggerFactory.getLogger(LocalAudioTrackExecutor.class);

  private final InternalAudioTrack audioTrack;
  private final AudioProcessingContext processingContext;
  private final boolean useSeekGhosting;
  private final AudioFrameBuffer frameBuffer;
  private final AtomicReference<Thread> playingThread = new AtomicReference<>();
  private final AtomicBoolean queuedStop = new AtomicBoolean(false);
  private final AtomicLong queuedSeek = new AtomicLong(-1);
  private final AtomicLong lastFrameTimecode = new AtomicLong(0);
  private final AtomicReference<AudioTrackState> state = new AtomicReference<>(AudioTrackState.INACTIVE);
  private final Object actionSynchronizer = new Object();
  private final TrackMarkerTracker markerTracker = new TrackMarkerTracker();
  private boolean interruptibleForSeek = false;
  private volatile Throwable trackException;

  /**
   * @param audioTrack The audio track that this executor executes
   * @param configuration Configuration to use for audio processing
   * @param playerOptions Mutable player options (for example volume).
   * @param useSeekGhosting Whether to keep providing old frames continuing from the previous position during a seek
   *                        until frames from the new position arrive.
   * @param bufferDuration The size of the frame buffer in milliseconds
   */
  public LocalAudioTrackExecutor(InternalAudioTrack audioTrack, AudioConfiguration configuration,
                                 AudioPlayerOptions playerOptions, boolean useSeekGhosting, int bufferDuration) {

    this.audioTrack = audioTrack;
    AudioDataFormat currentFormat = configuration.getOutputFormat();
    this.frameBuffer = configuration.getFrameBufferFactory().create(bufferDuration, currentFormat, queuedStop);
    this.processingContext = new AudioProcessingContext(configuration, frameBuffer, playerOptions, currentFormat);
    this.useSeekGhosting = useSeekGhosting;
  }

  public AudioProcessingContext getProcessingContext() {
    return processingContext;
  }

  @Override
  public AudioFrameBuffer getAudioBuffer() {
    return frameBuffer;
  }

  @Override
  public void execute(TrackStateListener listener) {
    InterruptedException interrupt = null;

    if (Thread.interrupted()) {
      log.debug("Cleared a stray interrupt.");
    }

    if (playingThread.compareAndSet(null, Thread.currentThread())) {
      log.debug("Starting to play track {} locally with listener {}", audioTrack.getInfo().identifier, listener);

      state.set(AudioTrackState.LOADING);

      try {
        audioTrack.process(this);

        log.debug("Playing track {} finished or was stopped.", audioTrack.getIdentifier());
      } catch (Throwable e) {
        // Temporarily clear the interrupted status so it would not disrupt listener methods.
        interrupt = findInterrupt(e);

        if (interrupt != null && checkStopped()) {
          log.debug("Track {} was interrupted outside of execution loop.", audioTrack.getIdentifier());
        } else {
          frameBuffer.setTerminateOnEmpty();

          FriendlyException exception = ExceptionTools.wrapUnfriendlyExceptions("Something broke when playing the track.", FAULT, e);
          ExceptionTools.log(log, exception, "playback of " + audioTrack.getIdentifier());

          trackException = exception;
          listener.onTrackException(audioTrack, exception);

          ExceptionTools.rethrowErrors(e);
        }
      } finally {
        synchronized (actionSynchronizer) {
          interrupt = interrupt != null ? interrupt : findInterrupt(null);

          playingThread.compareAndSet(Thread.currentThread(), null);

          markerTracker.trigger(ENDED);
          state.set(AudioTrackState.FINISHED);
        }

        if (interrupt != null) {
          Thread.currentThread().interrupt();
        }
      }
    } else {
      log.warn("Tried to start an already playing track {}", audioTrack.getIdentifier());
    }
  }

  @Override
  public void stop() {
    synchronized (actionSynchronizer) {
      Thread thread = playingThread.get();

      if (thread != null) {
        log.debug("Requesting stop for track {}", audioTrack.getIdentifier());

        queuedStop.compareAndSet(false, true);
        thread.interrupt();
      } else {
        log.debug("Tried to stop track {} which is not playing.", audioTrack.getIdentifier());
      }
    }
  }

  /**
   * @return True if the track has been scheduled to stop and then clears the scheduled stop bit.
   */
  public boolean checkStopped() {
    if (queuedStop.compareAndSet(true, false)) {
      state.set(AudioTrackState.STOPPING);
      return true;
    }

    return false;
  }

  /**
   * Wait until all the frames from the frame buffer have been consumed. Keeps the buffering thread alive to keep it
   * interruptible for seeking until buffer is empty.
   *
   * @throws InterruptedException When interrupted externally (or for seek/stop).
   */
  public void waitOnEnd() throws InterruptedException {
    frameBuffer.setTerminateOnEmpty();
    frameBuffer.waitForTermination();
  }

  /**
   * Interrupt the buffering thread, either stop or seek should have been set beforehand.
   * @return True if there was a thread to interrupt.
   */
  public boolean interrupt() {
    synchronized (actionSynchronizer) {
      Thread thread = playingThread.get();

      if (thread != null) {
        thread.interrupt();
        return true;
      }

      return false;
    }
  }

  @Override
  public long getPosition() {
    long seek = queuedSeek.get();
    return seek != -1 ? seek : lastFrameTimecode.get();
  }

  @Override
  public void setPosition(long timecode) {
    if (!audioTrack.isSeekable()) {
      return;
    }

    synchronized (actionSynchronizer) {
      if (timecode < 0) {
        timecode = 0;
      }

      queuedSeek.set(timecode);

      if (!useSeekGhosting) {
        frameBuffer.clear();
      }

      interruptForSeek();
    }
  }

  @Override
  public AudioTrackState getState() {
    return state.get();
  }

  /**
   * @return True if this track is currently in the middle of a seek.
   */
  private boolean isPerformingSeek() {
    return queuedSeek.get() != -1 || (useSeekGhosting && frameBuffer.hasClearOnInsert());
  }

  @Override
  public void setMarker(TrackMarker marker) {
    markerTracker.set(marker, getPosition());
  }

  @Override
  public boolean failedBeforeLoad() {
    return trackException != null && !frameBuffer.hasReceivedFrames();
  }

  /**
   * Execute the read and seek loop for the track.
   * @param readExecutor Callback for reading the track
   * @param seekExecutor Callback for performing a seek on the track, may be null on a non-seekable track
   */
  public void executeProcessingLoop(ReadExecutor readExecutor, SeekExecutor seekExecutor) {
    boolean proceed = true;

    if (checkPendingSeek(seekExecutor) == SeekResult.EXTERNAL_SEEK) {
      return;
    }

    while (proceed) {
      state.set(AudioTrackState.PLAYING);
      proceed = false;

      try {
        // An interrupt may have been placed while we were handling the previous one.
        if (Thread.interrupted() && !handlePlaybackInterrupt(null, seekExecutor)) {
          break;
        }

        setInterruptibleForSeek(true);
        readExecutor.performRead();
        setInterruptibleForSeek(false);

        // Must not finish before terminator frame has been consumed the user may still want to perform seeks until then
        waitOnEnd();
      } catch (Exception e) {
        setInterruptibleForSeek(false);
        InterruptedException interruption = findInterrupt(e);

        if (interruption != null) {
          proceed = handlePlaybackInterrupt(interruption, seekExecutor);
        } else {
          throw ExceptionTools.wrapUnfriendlyExceptions("Something went wrong when decoding the track.", FAULT, e);
        }
      }
    }
  }

  private void setInterruptibleForSeek(boolean state) {
    synchronized (actionSynchronizer) {
      interruptibleForSeek = state;
    }
  }

  private void interruptForSeek() {
    boolean interrupted = false;

    synchronized (actionSynchronizer) {
      if (interruptibleForSeek) {
        interruptibleForSeek = false;
        Thread thread = playingThread.get();

        if (thread != null) {
          thread.interrupt();
          interrupted = true;
        }
      }
    }

    if (interrupted) {
      log.debug("Interrupting playing thread to perform a seek {}", audioTrack.getIdentifier());
    } else {
      log.debug("Seeking on track {} while not in playback loop.", audioTrack.getIdentifier());
    }
  }

  private boolean handlePlaybackInterrupt(InterruptedException interruption, SeekExecutor seekExecutor) {
    Thread.interrupted();

    if (checkStopped()) {
      markerTracker.trigger(STOPPED);
      return false;
    }

    SeekResult seekResult = checkPendingSeek(seekExecutor);

    if (seekResult != SeekResult.NO_SEEK) {
      // Double-check, might have received a stop request while seeking
      if (checkStopped()) {
        markerTracker.trigger(STOPPED);
        return false;
      } else {
        return seekResult == SeekResult.INTERNAL_SEEK;
      }
    } else if (interruption != null) {
      Thread.currentThread().interrupt();
      throw new FriendlyException("The track was unexpectedly terminated.", SUSPICIOUS, interruption);
    } else {
      return true;
    }
  }

  private InterruptedException findInterrupt(Throwable throwable) {
    InterruptedException exception = findDeepException(throwable, InterruptedException.class);

    if (exception == null) {
      InterruptedIOException ioException = findDeepException(throwable, InterruptedIOException.class);

      if (ioException != null && (ioException.getMessage() == null || !ioException.getMessage().contains("timed out"))) {
        exception = new InterruptedException(ioException.getMessage());
      }
    }

    if (exception == null && Thread.interrupted()) {
      return new InterruptedException();
    }

    return exception;
  }

  /**
   * Performs a seek if it scheduled.
   * @param seekExecutor Callback for performing a seek on the track
   * @return True if a seek was performed
   */
  private SeekResult checkPendingSeek(SeekExecutor seekExecutor) {
    if (!audioTrack.isSeekable()) {
      return SeekResult.NO_SEEK;
    }

    long seekPosition;

    synchronized (actionSynchronizer) {
      seekPosition = queuedSeek.get();

      if (seekPosition == -1) {
        return SeekResult.NO_SEEK;
      }

      log.debug("Track {} interrupted for seeking to {}.", audioTrack.getIdentifier(), seekPosition);
      applySeekState(seekPosition);
    }

    if (seekExecutor != null) {
      try {
        seekExecutor.performSeek(seekPosition);
      } catch (Exception e) {
        throw ExceptionTools.wrapUnfriendlyExceptions("Something went wrong when seeking to a position.", FAULT, e);
      }

      return SeekResult.INTERNAL_SEEK;
    } else {
      return SeekResult.EXTERNAL_SEEK;
    }
  }

  private void applySeekState(long seekPosition) {
    state.set(AudioTrackState.SEEKING);

    if (useSeekGhosting) {
      frameBuffer.setClearOnInsert();
    } else {
      frameBuffer.clear();
    }

    queuedSeek.set(-1);
    markerTracker.checkSeekTimecode(seekPosition);
  }

  @Override
  public AudioFrame provide() {
    AudioFrame frame = frameBuffer.provide();
    processProvidedFrame(frame);
    return frame;
  }

  @Override
  public AudioFrame provide(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
    AudioFrame frame = frameBuffer.provide(timeout, unit);
    processProvidedFrame(frame);
    return frame;
  }

  @Override
  public boolean provide(MutableAudioFrame targetFrame) {
    if (frameBuffer.provide(targetFrame)) {
      processProvidedFrame(targetFrame);
      return true;
    }

    return false;
  }

  @Override
  public boolean provide(MutableAudioFrame targetFrame, long timeout, TimeUnit unit)
      throws TimeoutException, InterruptedException {

    if (frameBuffer.provide(targetFrame, timeout, unit)) {
      processProvidedFrame(targetFrame);
      return true;
    }

    return true;
  }

  private void processProvidedFrame(AudioFrame frame) {
    if (frame != null && !frame.isTerminator()) {
      if (!isPerformingSeek()) {
        markerTracker.checkPlaybackTimecode(frame.getTimecode());
      }

      lastFrameTimecode.set(frame.getTimecode());
    }
  }

  /**
   * Read executor, see method description
   */
  public interface ReadExecutor {
    /**
     * Reads until interrupted or EOF.
     *
     * @throws InterruptedException When interrupted externally (or for seek/stop).
     */
    void performRead() throws InterruptedException;
  }

  /**
   * Seek executor, see method description
   */
  public interface SeekExecutor {
    /**
     * Perform a seek to the specified position
     *
     * @param position Position in milliseconds
     */
    void performSeek(long position);
  }

  private enum SeekResult {
    NO_SEEK,
    INTERNAL_SEEK,
    EXTERNAL_SEEK
  }
}
