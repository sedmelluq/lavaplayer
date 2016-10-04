package com.sedmelluq.discord.lavaplayer.track.playback;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.TrackExceptionEvent;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioLoop;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.FAULT;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Handles the execution and output buffering of an audio track.
 */
public class AudioTrackExecutor implements AudioFrameProvider {
  private static final Logger log = LoggerFactory.getLogger(AudioTrackExecutor.class);

  private static final int BUFFER_DURATION_MS = 5000;

  private final String identifier;

  private final AudioFrameBuffer frameBuffer = new AudioFrameBuffer(BUFFER_DURATION_MS);
  private final AtomicReference<InternalAudioTrack> rootTrack = new AtomicReference<>();
  private final AtomicReference<Thread> playingThread = new AtomicReference<>();
  private final AtomicReference<AudioPlayer> currentPlayer = new AtomicReference<>();
  private final AtomicBoolean isStopping = new AtomicBoolean(false);
  private final AtomicLong pendingSeek = new AtomicLong(-1);
  private final AtomicLong lastFrameTimecode = new AtomicLong(0);
  private final AtomicReference<AudioTrackState> state = new AtomicReference<>(AudioTrackState.INACTIVE);
  private final Object actionSynchronizer = new Object();
  private volatile AudioLoop audioLoop;

  /**
   * @param identifier Audio track identifier.
   */
  public AudioTrackExecutor(String identifier) {
    this.identifier = identifier;
  }

  /**
   * @return The unique identifier for the associated track.
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * @return The frame consumer for samples of this track.
   */
  public AudioFrameConsumer getFrameConsumer() {
    return frameBuffer;
  }

  /**
   * Assign the associated audio track to the executor, only the first call counts.
   * @param track The track to assign to this executor
   */
  public void assign(InternalAudioTrack track) {
    this.rootTrack.compareAndSet(null, track);
  }

  /**
   * Execute the track, which means that this thread will fill the frame buffer until the track finishes or is stopped.
   * @param player The AudioPlayer which is running this executor
   * @param volumeLevel Mutable volume level to use when executing the track
   */
  public void execute(AudioPlayer player, AtomicInteger volumeLevel) {
    InternalAudioTrack audioTrack = rootTrack.get();

    if (playingThread.compareAndSet(null, Thread.currentThread())) {
      state.set(AudioTrackState.LOADING);
      currentPlayer.set(player);

      try {
        audioTrack.process(volumeLevel);

        log.info("Playing track {} finished or was stopped.", audioTrack.getIdentifier());
      } catch (Throwable e) {
        FriendlyException exception = ExceptionTools.wrapUnfriendlyExceptions("Something broke when playing the track.", FAULT, e);
        ExceptionTools.log(log, exception, "playback of " + getIdentifier());

        player.dispatchEvent(new TrackExceptionEvent(player, audioTrack, exception));

        ExceptionTools.rethrowErrors(e);
      } finally {
        frameBuffer.setTerminateOnEmpty();

        synchronized (actionSynchronizer) {
          Thread.interrupted();

          currentPlayer.set(null);
          playingThread.compareAndSet(Thread.currentThread(), null);

          state.set(AudioTrackState.INACTIVE);
        }
      }
    } else {
      log.warn("Tried to start an already playing track {}", audioTrack.getIdentifier());
    }
  }

  /**
   * Stop playing the track, terminating the thread that is filling the frame buffer.
   */
  public void stop() {
    synchronized (actionSynchronizer) {
      InternalAudioTrack audioTrack = rootTrack.get();
      Thread thread = playingThread.get();

      if (thread != null) {
        log.debug("Requesting stop for track {}", audioTrack.getIdentifier());

        isStopping.compareAndSet(false, true);
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
    return isStopping.compareAndSet(true, false);
  }

  /**
   * Wait until all the frames from the frame buffer have been consumed. Keeps the buffering thread alive to keep it
   * interruptible for seeking until buffer is empty.
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

  /**
   * @return Timecode of the last played frame or in case a seek is in progress, the timecode of the frame being seeked to.
   */
  public long getPosition() {
    long seek = pendingSeek.get();
    return seek != -1 ? seek : lastFrameTimecode.get();
  }

  /**
   * Perform seek to the specified timecode.
   * @param timecode The timecode in milliseconds
   */
  public void setPosition(long timecode) {
    synchronized (actionSynchronizer) {
      if (timecode < 0) {
        timecode = 0;
      }

      pendingSeek.set(timecode);

      if (interrupt()) {
        log.debug("Interrupting playing thread to perform a seek {}", getIdentifier());
      } else {
        log.debug("Seeking on a track which is not playing {}", getIdentifier());
      }
    }
  }

  /**
   * @return Current state of the executor
   */
  public AudioTrackState getState() {
    return state.get();
  }

  /**
   * @return True if this track is currently in the middle of a seek.
   */
  private boolean isPerformingSeek() {
    return pendingSeek.get() != -1 || frameBuffer.hasClearOnInsert();
  }

  /**
   * Set the playback to loop between specified timecodes.
   * @param loop The loop to set. Use null to cancel a previous loop.
   */
  public void setLoop(AudioLoop loop) {
    this.audioLoop = loop;

    if (loop != null) {
      log.debug("Setting loop between {} and {} on track {}", loop.startPosition, loop.endPosition, getIdentifier());
      setPosition(loop.startPosition);
    }
  }

  /**
   * Execute the read and seek loop for the track.
   * @param readExecutor Callback for reading the track
   * @param seekExecutor Callback for performing a seek on the track
   */
  public void executeProcessingLoop(ReadExecutor readExecutor, SeekExecutor seekExecutor) {
    boolean proceed = true;

    while (proceed) {
      state.set(AudioTrackState.PLAYING);
      proceed = false;

      try {
        readExecutor.performRead();

        // Must not finish before terminator frame has been consumed the user may still want to perform seeks until then
        waitOnEnd();
      } catch (InterruptedException interruption) {
        Thread.interrupted();

        if (checkStopped()) {
          proceed = false;
        } else if (applyPendingSeek(seekExecutor)) {
          proceed = true;
        } else {
          throw new FriendlyException("The track was unexpectedly terminated.", SUSPICIOUS, interruption);
        }
      } catch (Exception e) {
        throw ExceptionTools.wrapUnfriendlyExceptions("Something went wrong when decoding the track.", FAULT, e);
      }
    }
  }

  /**
   * Performs a seek if it scheduled.
   * @param seekExecutor Callback for performing a seek on the track
   * @return True if a seek was performed
   */
  private boolean applyPendingSeek(SeekExecutor seekExecutor) {
    synchronized (actionSynchronizer) {
      long seek = pendingSeek.get();

      if (seek != -1) {
        log.debug("Track {} interrupted for seeking to {}.", getIdentifier(), seek);

        try {
          state.set(AudioTrackState.SEEKING);

          frameBuffer.setClearOnInsert();
          seekExecutor.performSeek(seek);
          pendingSeek.set(-1);
        } catch (Exception e) {
          throw ExceptionTools.wrapUnfriendlyExceptions("Something went wrong when seeking to a position.", FAULT, e);
        }

        return true;
      }
    }

    return false;
  }

  @Override
  public AudioFrame provide() {
    AudioFrame frame = frameBuffer.provide();

    if (frame != null && !frame.isTerminator()) {
      AudioLoop loop = audioLoop;
      if (loop != null && frame.timecode >= loop.endPosition && !isPerformingSeek()) {
        setPosition(loop.startPosition);
      }

      lastFrameTimecode.set(frame.timecode);
    }

    return frame;
  }

  /**
   * Read executor, see method description
   */
  public interface ReadExecutor {
    /**
     * Reads until interrupted or EOF
     * @throws InterruptedException
     */
    void performRead() throws InterruptedException;
  }

  /**
   * Seek executor, see method description
   */
  public interface SeekExecutor {
    /**
     * Perform a seek to the specified position
     * @param position Position in milliseconds
     */
    void performSeek(long position);
  }
}
