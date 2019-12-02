package com.sedmelluq.lavaplayer.core.player.playback;

import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.player.configuration.AudioConfiguration;
import com.sedmelluq.lavaplayer.core.player.frame.AudioFrame;
import com.sedmelluq.lavaplayer.core.player.frame.AudioFrameBuffer;
import com.sedmelluq.lavaplayer.core.player.frame.AudioFrameBufferFactory;
import com.sedmelluq.lavaplayer.core.player.frame.MutableAudioFrame;
import com.sedmelluq.lavaplayer.core.player.marker.TrackMarker;
import com.sedmelluq.lavaplayer.core.player.marker.TrackMarkerTracker;
import com.sedmelluq.lavaplayer.core.player.playback.configuration.PlaybackAudioConfiguration;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrackState;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrackStateListener;
import com.sedmelluq.lavaplayer.core.player.track.ExecutableAudioTrack;
import com.sedmelluq.lavaplayer.core.source.AudioSource;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import com.sedmelluq.lavaplayer.core.tools.userdata.AbstractUserDataHolder;
import java.io.InterruptedIOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.player.marker.TrackMarkerHandler.MarkerState.ENDED;
import static com.sedmelluq.lavaplayer.core.player.marker.TrackMarkerHandler.MarkerState.STOPPED;
import static com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools.findDeepException;
import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.FAULT;
import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.SUSPICIOUS;

public class PlaybackAudioTrack extends AbstractUserDataHolder implements ExecutableAudioTrack, AudioPlaybackController {
  private static final Logger log = LoggerFactory.getLogger(PlaybackAudioTrack.class);

  private final AudioTrackInfo trackInfo;
  private final AudioPlayback playback;
  private final AudioFrameBuffer frameBuffer;
  private final AudioPlaybackContext context;
  private final AudioSource sourceManager;
  private final ExecutorService executorService;
  private final AtomicReference<Thread> playingThread = new AtomicReference<>();
  private final AtomicBoolean queuedStop = new AtomicBoolean(false);
  private final AtomicLong queuedSeek = new AtomicLong(-1);
  private final AtomicLong lastFrameTimecode = new AtomicLong(0);
  private final AtomicReference<AudioTrackState> state = new AtomicReference<>(AudioTrackState.INACTIVE);
  private final Object actionSynchronizer = new Object();
  private final TrackMarkerTracker markerTracker = new TrackMarkerTracker();
  private long externalSeekPosition = -1;
  private boolean interruptibleForSeek = false;
  private volatile long trackDuration;
  private volatile Throwable trackException;

  public PlaybackAudioTrack(
      AudioTrackInfo trackInfo,
      AudioPlayback playback,
      AudioConfiguration configuration,
      AudioFrameBufferFactory frameBufferFactory,
      AudioSource sourceManager,
      ExecutorService executorService
  ) {
    this.trackInfo = trackInfo;
    this.playback = playback;
    this.sourceManager = sourceManager;
    this.executorService = executorService;

    PlaybackAudioConfiguration activeConfiguration = new PlaybackAudioConfiguration(configuration);

    this.frameBuffer = frameBufferFactory.create(
        activeConfiguration.getFrameBufferDuration(),
        activeConfiguration.getOutputFormat(),
        queuedStop
    );

    this.context = new DefaultAudioPlaybackContext(activeConfiguration, frameBuffer);

    this.trackDuration = trackInfo.getLength();
  }

  @Override
  public void execute(AudioTrackStateListener listener) {
    executorService.execute(() -> executeSynchronously(listener));
  }

  @Override
  public void stop() {
    synchronized (actionSynchronizer) {
      Thread thread = playingThread.get();

      if (thread != null) {
        log.debug("Requesting stop for track {}", trackInfo.getIdentifier());

        queuedStop.compareAndSet(false, true);
        thread.interrupt();
      } else {
        log.debug("Tried to stop track {} which is not playing.", trackInfo.getIdentifier());
      }
    }
  }

  @Override
  public boolean failedBeforeLoad() {
    return trackException != null && !context.getFrameBuffer().hasReceivedFrames();
  }

  @Override
  public AudioTrackInfo getInfo() {
    return trackInfo;
  }

  @Override
  public AudioTrackState getState() {
    return state.get();
  }

  @Override
  public boolean isSeekable() {
    return !trackInfo.isStream();
  }

  @Override
  public long getPosition() {
    long seek = queuedSeek.get();
    return seek != -1 ? seek : lastFrameTimecode.get();
  }

  @Override
  public void setPosition(long position) {
    if (!isSeekable()) {
      return;
    }

    synchronized (actionSynchronizer) {
      if (position < 0) {
        position = 0;
      }

      queuedSeek.set(position);

      if (!context.getConfiguration().isUsingSeekGhosting()) {
        context.getFrameBuffer().clear();
      }

      interruptForSeek();
    }
  }

  @Override
  public void setMarker(TrackMarker marker) {
    markerTracker.set(marker, getPosition());
  }

  @Override
  public long getDuration() {
    return trackDuration;
  }

  @Override
  public AudioSource getSource() {
    return sourceManager;
  }

  @Override
  public AudioPlaybackContext getContext() {
    return context;
  }

  @Override
  public void executeProcessingLoop(ReadExecutor readExecutor, SeekExecutor seekExecutor, boolean waitOnEnd) {
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
        if (seekExecutor != null && externalSeekPosition != -1) {
          long nextPosition = externalSeekPosition;
          externalSeekPosition = -1;
          performSeek(seekExecutor, nextPosition);
          proceed = true;
        } else if (waitOnEnd) {
          waitOnEnd();
        }
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

  @Override
  public void updateDuration(long duration) {
    trackDuration = duration;
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

  private boolean isPerformingSeek() {
    return queuedSeek.get() != -1 ||
        (context.getConfiguration().isUsingSeekGhosting() && frameBuffer.hasClearOnInsert());
  }

  private void executeSynchronously(AudioTrackStateListener stateListener) {
    InterruptedException interrupt = null;

    if (Thread.interrupted()) {
      log.debug("Cleared a stray interrupt.");
    }

    if (playingThread.compareAndSet(null, Thread.currentThread())) {
      log.debug("Starting to play track {} locally with listener {}", trackInfo.getIdentifier(), stateListener);

      state.set(AudioTrackState.LOADING);

      try {
        playback.process(this);

        log.debug("Playing track {} finished or was stopped.", trackInfo.getIdentifier());
      } catch (Throwable e) {
        // Temporarily clear the interrupted status so it would not disrupt listener methods.
        interrupt = findInterrupt(e);

        if (interrupt != null && checkStopped()) {
          log.debug("Track {} was interrupted outside of execution loop.", trackInfo.getIdentifier());
        } else {
          context.getFrameBuffer().setTerminateOnEmpty();

          FriendlyException exception = ExceptionTools
              .wrapUnfriendlyExceptions("Something broke when playing the track.", FAULT, e);

          ExceptionTools.log(log, exception, "playback of " + trackInfo.getIdentifier());

          trackException = exception;
          stateListener.onTrackException(this, exception);

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
      log.warn("Tried to start an already playing track {}", trackInfo.getIdentifier());
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

  private boolean checkStopped() {
    if (queuedStop.compareAndSet(true, false)) {
      state.set(AudioTrackState.STOPPING);
      return true;
    }

    return false;
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
      log.debug("Interrupting playing thread to perform a seek {}", trackInfo.getIdentifier());
    } else {
      log.debug("Seeking on track {} while not in playback loop.", trackInfo.getIdentifier());
    }
  }

  /**
   * Performs a seek if it scheduled.
   * @param seekExecutor Callback for performing a seek on the track
   * @return True if a seek was performed
   */
  private SeekResult checkPendingSeek(SeekExecutor seekExecutor) {
    if (!isSeekable()) {
      return SeekResult.NO_SEEK;
    }

    long seekPosition;

    synchronized (actionSynchronizer) {
      seekPosition = queuedSeek.get();

      if (seekPosition == -1) {
        return SeekResult.NO_SEEK;
      }

      log.debug("Track {} interrupted for seeking to {}.", trackInfo.getIdentifier(), seekPosition);
      applySeekState(seekPosition);
    }

    if (seekExecutor != null) {
      performSeek(seekExecutor, seekPosition);
      return SeekResult.INTERNAL_SEEK;
    } else {
      return SeekResult.EXTERNAL_SEEK;
    }
  }

  private void performSeek(SeekExecutor seekExecutor, long seekPosition) {
    try {
      seekExecutor.performSeek(seekPosition);
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions("Something went wrong when seeking to a position.", FAULT, e);
    }
  }

  private void applySeekState(long seekPosition) {
    state.set(AudioTrackState.SEEKING);

    if (context.getConfiguration().isUsingSeekGhosting()) {
      context.getFrameBuffer().setClearOnInsert();
    } else {
      context.getFrameBuffer().clear();
    }

    queuedSeek.set(-1);
    markerTracker.checkSeekTimecode(seekPosition);
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

  private void setInterruptibleForSeek(boolean state) {
    synchronized (actionSynchronizer) {
      interruptibleForSeek = state;
    }
  }

  /**
   * Wait until all the frames from the frame buffer have been consumed. Keeps the buffering thread alive to keep it
   * interruptible for seeking until buffer is empty.
   *
   * @throws InterruptedException When interrupted externally (or for seek/stop).
   */
  private void waitOnEnd() throws InterruptedException {
    context.getFrameBuffer().setTerminateOnEmpty();
    context.getFrameBuffer().waitForTermination();
  }

  private enum SeekResult {
    NO_SEEK,
    INTERNAL_SEEK,
    EXTERNAL_SEEK
  }
}
