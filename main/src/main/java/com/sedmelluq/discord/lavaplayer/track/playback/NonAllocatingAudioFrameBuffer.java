package com.sedmelluq.discord.lavaplayer.track.playback;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Audio frame buffer implementation which never allocates any new objects after creation. All instances of mutable
 * frames are preallocated, and for the data there is one byte buffer which is used as a ring buffer for the frame data.
 */
public class NonAllocatingAudioFrameBuffer extends AbstractAudioFrameBuffer {
  private static final Logger log = LoggerFactory.getLogger(NonAllocatingAudioFrameBuffer.class);

  private final int worstCaseFrameCount;
  private final ReferenceMutableAudioFrame[] frames;
  private final ReferenceMutableAudioFrame silentFrame;
  private final AtomicBoolean stopping;
  private MutableAudioFrame bridgeFrame;

  private final byte[] frameBuffer;
  private int firstFrame;
  private int frameCount;

  /**
   * @param bufferDuration The length of the internal buffer in milliseconds
   * @param format The format of the frames held in this buffer
   * @param stopping Atomic boolean which has true value when the track is in a state of pending stop.
   */
  public NonAllocatingAudioFrameBuffer(int bufferDuration, AudioDataFormat format, AtomicBoolean stopping) {
    super(format);
    int maximumFrameCount = bufferDuration / (int) format.frameDuration() + 1;
    frames = createFrames(maximumFrameCount, format);
    silentFrame = createSilentFrame(format);
    this.frameBuffer = new byte[format.expectedChunkSize() * maximumFrameCount];
    worstCaseFrameCount = frameBuffer.length / format.maximumChunkSize();
    this.stopping = stopping;
  }

  /**
   * @return Number of frames that can be added to the buffer without blocking.
   */
  @Override
  public int getRemainingCapacity() {
    synchronized (synchronizer) {
      if (frameCount == 0) {
        return worstCaseFrameCount;
      }

      int lastFrame = wrappedFrameIndex(firstFrame + frameCount - 1);

      int bufferHead = frames[firstFrame].getFrameOffset();
      int bufferTail = frames[lastFrame].getFrameEndOffset();

      int maximumFrameSize = format.maximumChunkSize();

      if (bufferHead < bufferTail) {
        return (frameBuffer.length - bufferTail) / maximumFrameSize + bufferHead / maximumFrameSize;
      } else {
        return (bufferHead - bufferTail) / maximumFrameSize;
      }
    }
  }

  /**
   * @return Total number of frames that the buffer can hold.
   */
  @Override
  public int getFullCapacity() {
    return worstCaseFrameCount;
  }

  @Override
  public void consume(AudioFrame frame) throws InterruptedException {
    // If an interrupt sent along with setting the stopping status was silently consumed elsewhere, this check should
    // still trigger. Guarantees that stopped tracks cannot get stuck in this method. Possible performance improvement:
    // offer with timeout, check stopping if timed out, then put?
    if (stopping != null && stopping.get()) {
      throw new InterruptedException();
    }

    synchronized (synchronizer) {
      if (!locked) {
        receivedFrames = true;

        if (clearOnInsert) {
          clear();
          clearOnInsert = false;
        }

        while (!attemptStore(frame)) {
          synchronizer.wait();
        }

        synchronizer.notifyAll();
      }
    }
  }

  @Override
  public AudioFrame provide() {
    synchronized (synchronizer) {
      if (provide(getBridgeFrame())) {
        return unwrapBridgeFrame();
      }

      return null;
    }
  }

  @Override
  public AudioFrame provide(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
    synchronized (synchronizer) {
      if (provide(getBridgeFrame(), timeout, unit)) {
        return unwrapBridgeFrame();
      }

      return null;
    }
  }

  @Override
  public boolean provide(MutableAudioFrame targetFrame) {
    synchronized (synchronizer) {
      if (frameCount == 0) {
        if (terminateOnEmpty) {
          popPendingTerminator(targetFrame);
          synchronizer.notifyAll();
          return true;
        }
        return false;
      } else {
        popFrame(targetFrame);
        synchronizer.notifyAll();
        return true;
      }
    }
  }

  @Override
  public boolean provide(MutableAudioFrame targetFrame, long timeout, TimeUnit unit)
      throws TimeoutException, InterruptedException {

    long currentTime = System.nanoTime();
    long endTime = currentTime + unit.toMillis(timeout);

    synchronized (synchronizer) {
      while (frameCount == 0) {
        if (terminateOnEmpty) {
          popPendingTerminator(targetFrame);
          synchronizer.notifyAll();
          return true;
        }

        synchronizer.wait(endTime - currentTime);
        currentTime = System.nanoTime();

        if (currentTime >= endTime) {
          throw new TimeoutException();
        }
      }

      popFrame(targetFrame);
      synchronizer.notifyAll();
      return true;
    }
  }

  private void popFrame(MutableAudioFrame targetFrame) {
    ReferenceMutableAudioFrame frame = frames[firstFrame];

    if (frame.getVolume() == 0) {
      silentFrame.setTimecode(frame.getTimecode());
      frame = silentFrame;
    }

    targetFrame.setTimecode(frame.getTimecode());
    targetFrame.setVolume(frame.getVolume());
    targetFrame.setTerminator(false);
    targetFrame.store(frame.getFrameBuffer(), frame.getFrameOffset(), frame.getDataLength());

    firstFrame = wrappedFrameIndex(firstFrame + 1);
    frameCount--;
  }

  private void popPendingTerminator(MutableAudioFrame frame) {
    terminateOnEmpty = false;
    terminated = true;

    frame.setTerminator(true);
  }

  @Override
  public void clear() {
    synchronized (synchronizer) {
      frameCount = 0;
    }
  }

  @Override
  public void rebuild(AudioFrameRebuilder rebuilder) {
    log.debug("Frame rebuild not supported on non-allocating frame buffer yet.");
  }

  @Override
  public Long getLastInputTimecode() {
    synchronized (synchronizer) {
      if (!clearOnInsert && frameCount > 0) {
        return frames[wrappedFrameIndex(firstFrame + frameCount - 1)].getTimecode();
      }
    }

    return null;
  }

  private boolean attemptStore(AudioFrame frame) {
    if (frameCount >= frames.length) {
      return false;
    }

    int frameLength = frame.getDataLength();
    int frameBufferLength = frameBuffer.length;

    if (frameCount == 0) {
      firstFrame = 0;

      if (frameLength > frameBufferLength) {
        throw new IllegalArgumentException("Frame is too big for buffer.");
      }

      store(frame, 0, 0, frameLength);
    } else {
      int lastFrame = wrappedFrameIndex(firstFrame + frameCount - 1);
      int nextFrame = wrappedFrameIndex(lastFrame + 1);

      int bufferHead = frames[firstFrame].getFrameOffset();
      int bufferTail = frames[lastFrame].getFrameEndOffset();

      if (bufferHead < bufferTail) {
        if (bufferTail + frameLength <= frameBufferLength) {
          store(frame, nextFrame, bufferTail, frameLength);
        } else if (bufferHead >= frameLength) {
          store(frame, nextFrame, 0, frameLength);
        } else {
          return false;
        }
      } else if (bufferTail + frameLength <= bufferHead) {
        store(frame, nextFrame, bufferTail, frameLength);
      } else {
        return false;
      }
    }

    return true;
  }

  private int wrappedFrameIndex(int index) {
    int maximumFrameCount = frames.length;
    return index >= maximumFrameCount ? index - maximumFrameCount : index;
  }

  private void store(AudioFrame frame, int index, int frameOffset, int frameLength) {
    ReferenceMutableAudioFrame targetFrame = frames[index];
    targetFrame.setTimecode(frame.getTimecode());
    targetFrame.setVolume(frame.getVolume());
    targetFrame.setDataReference(frameBuffer, frameOffset, frameLength);

    frame.getData(frameBuffer, frameOffset);

    frameCount++;
  }

  private MutableAudioFrame getBridgeFrame() {
    if (bridgeFrame == null) {
      bridgeFrame = new MutableAudioFrame();
      bridgeFrame.setBuffer(ByteBuffer.allocate(format.maximumChunkSize()));
    }

    return bridgeFrame;
  }

  private AudioFrame unwrapBridgeFrame() {
    if (bridgeFrame.isTerminator()) {
      return TerminatorAudioFrame.INSTANCE;
    } else {
      return new ImmutableAudioFrame(bridgeFrame.getTimecode(), bridgeFrame.getData(), bridgeFrame.getVolume(),
          bridgeFrame.getFormat());
    }
  }

  private static ReferenceMutableAudioFrame[] createFrames(int frameCount, AudioDataFormat format) {
    ReferenceMutableAudioFrame[] frames = new ReferenceMutableAudioFrame[frameCount];

    for (int i = 0; i < frames.length; i++) {
      frames[i] = new ReferenceMutableAudioFrame();
      frames[i].setFormat(format);
    }

    return frames;
  }

  private static ReferenceMutableAudioFrame createSilentFrame(AudioDataFormat format) {
    ReferenceMutableAudioFrame frame = new ReferenceMutableAudioFrame();
    frame.setFormat(format);
    frame.setDataReference(format.silenceBytes(), 0, format.silenceBytes().length);
    frame.setVolume(0);
    return frame;
  }
}
