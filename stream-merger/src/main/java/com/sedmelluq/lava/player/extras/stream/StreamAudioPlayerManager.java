package com.sedmelluq.lava.player.extras.stream;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.ExecutorTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.sedmelluq.discord.lavaplayer.tools.DataFormatTools.defaultOnNull;

public class StreamAudioPlayerManager extends DefaultAudioPlayerManager {
  private static final Logger log = LoggerFactory.getLogger(StreamAudioPlayerManager.class);

  private final Map<String, StreamInstance> streams;
  private final Predicate<AudioTrack> condition;
  private final ResolutionCache resolutionCache;
  private final int streamFrameCount;

  public StreamAudioPlayerManager(Predicate<AudioTrack> condition, int streamFrameCount) {
    this.streams = new HashMap<>();
    this.condition = condition;
    this.resolutionCache = new ResolutionCache(1000);
    this.streamFrameCount = streamFrameCount;
  }

  @Override
  public AudioPlayer createPlayer() {
    return new StreamAudioPlayer(super.createPlayer(), this);
  }

  @Override
  public Future<Void> loadItem(String identifier, AudioLoadResultHandler resultHandler) {
    if (loadFromStream(identifier, resultHandler)) {
      return ExecutorTools.COMPLETED_VOID;
    }

    return super.loadItem(identifier, new SpyingHandler(resultHandler, identifier));
  }

  @Override
  public Future<Void> loadItemOrdered(Object orderingKey, String identifier, AudioLoadResultHandler resultHandler) {
    if (loadFromStream(identifier, resultHandler)) {
      // This bypasses the ordering but oh well.
      return ExecutorTools.COMPLETED_VOID;
    }

    return super.loadItemOrdered(orderingKey, identifier, resultHandler);
  }

  public StreamInstance.Cursor openTrack(AudioTrack track, Consumer<StreamInstance.Cursor> detachListener) {
    synchronized (streams) {
      StreamInstance instance = streams.get(track.getIdentifier());

      if (instance != null) {
        StreamInstance.Cursor cursor = instance.createCursor(detachListener);

        if (cursor != null) {
          return cursor;
        } else {
          streams.remove(track.getIdentifier());
        }
      }

      if (!condition.test(track)) {
        return null;
      }

      instance = new StreamInstance(track, super.createPlayer(), streamFrameCount);
      streams.put(track.getIdentifier(), instance);

      return instance.createCursor(detachListener);
    }
  }

  private boolean loadFromStream(String identifier, AudioLoadResultHandler resultHandler) {
    try {
      StreamInstance stream;

      synchronized (streams) {
        String finalIdentifier = defaultOnNull(resolutionCache.get(identifier), identifier);
        stream = streams.get(finalIdentifier);
      }

      if (stream != null) {
        AudioTrack track = stream.getTrack().makeClone();
        log.debug("Track {} (originally {}) loaded using existing stream.", track.getIdentifier(), identifier);

        resultHandler.trackLoaded(track);
        return true;
      }
    } catch (Exception e) {
      log.error("Error when checking streams for identifier {}.", identifier);
    }

    return false;
  }

  private class SpyingHandler implements AudioLoadResultHandler {
    private final AudioLoadResultHandler delegate;
    private final String identifier;

    private SpyingHandler(AudioLoadResultHandler delegate, String identifier) {
      this.delegate = delegate;
      this.identifier = identifier;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
      if (condition.test(track) && !track.getIdentifier().equals(identifier)) {
        synchronized (streams) {
          resolutionCache.put(identifier, track.getIdentifier());
        }
      }

      delegate.trackLoaded(track);
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
      delegate.playlistLoaded(playlist);
    }

    @Override
    public void noMatches() {
      delegate.noMatches();
    }

    @Override
    public void loadFailed(FriendlyException exception) {
      delegate.loadFailed(exception);
    }
  }

  private static class ResolutionCache extends LinkedHashMap<String, String> {
    private final int maximumCacheSize;

    public ResolutionCache(int maximumCacheSize) {
      super(100, 0.75f, true);
      this.maximumCacheSize = maximumCacheSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
      return size() >= maximumCacheSize;
    }
  }
}
