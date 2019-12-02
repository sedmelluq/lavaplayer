package com.sedmelluq.lavaplayer.core.info.loader;

import com.sedmelluq.lava.common.tools.DaemonThreadFactory;
import com.sedmelluq.lava.common.tools.ExecutorTools;
import com.sedmelluq.lavaplayer.core.info.AudioInfoEntity;
import com.sedmelluq.lavaplayer.core.info.playlist.AudioPlaylist;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.source.AudioSource;
import com.sedmelluq.lavaplayer.core.source.AudioSourceRegistry;
import com.sedmelluq.lavaplayer.core.tools.OrderedExecutor;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.FAULT;
import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.SUSPICIOUS;

public class DefaultAudioInfoRequestHandler implements AudioInfoRequestHandler {
  private static final Logger log = LoggerFactory.getLogger(DefaultAudioInfoRequestHandler.class);

  private static final int MAXIMUM_LOAD_REDIRECTS = 5;
  private static final int DEFAULT_LOADER_POOL_SIZE = 10;
  private static final int LOADER_QUEUE_CAPACITY = 5000;

  private final AudioSourceRegistry sourceRegistry;
  private final ThreadPoolExecutor trackInfoExecutorService;
  private final OrderedExecutor orderedInfoExecutor;

  public DefaultAudioInfoRequestHandler(AudioSourceRegistry sourceRegistry) {
    this.sourceRegistry = sourceRegistry;
    trackInfoExecutorService = ExecutorTools.createEagerlyScalingExecutor(1, DEFAULT_LOADER_POOL_SIZE,
        TimeUnit.SECONDS.toMillis(30), LOADER_QUEUE_CAPACITY, new DaemonThreadFactory("info-loader"));
    orderedInfoExecutor = new OrderedExecutor(trackInfoExecutorService);
  }

  @Override
  public Future<Void> request(AudioInfoRequest request) {
    try {
      Callable<Void> loader = createItemLoader(request);
      Object orderingKey = request.getOrderChannelKey();

      if (orderingKey != null) {
        return orderedInfoExecutor.submit(orderingKey, loader);
      } else {
        return trackInfoExecutorService.submit(loader);
      }
    } catch (RejectedExecutionException e) {
      return handleLoadRejected(request, e);
    }
  }

  @Override
  public void close() {
    ExecutorTools.shutdownExecutor(trackInfoExecutorService, "track info");
  }

  protected Future<Void> handleLoadRejected(AudioInfoRequest request, RejectedExecutionException e) {
    FriendlyException exception = new FriendlyException("Cannot queue loading a track, queue is full.", SUSPICIOUS, e);
    ExceptionTools.log(log, exception, "queueing item " + request.name());

    request.getResponseHandler().loadFailed(exception);

    return ExecutorTools.COMPLETED_VOID;
  }

  protected Callable<Void> createItemLoader(AudioInfoRequest request) {
    return () -> {
      LookupContext context = new LookupContext(request.getResponseHandler());

      try {
        if (!checkSourcesForItem(request, context)) {
          log.debug("No matches for track with identifier {}.", request.name());
          request.getResponseHandler().noMatches();
        }
      } catch (Throwable throwable) {
        if (context.reported) {
          log.warn("Load result handler for {} threw an exception", request.name(), throwable);
        } else {
          dispatchItemLoadFailure(request, throwable);
        }

        ExceptionTools.rethrowErrors(throwable);
      }

      return null;
    };
  }

  protected void dispatchItemLoadFailure(AudioInfoRequest request, Throwable throwable) {
    FriendlyException exception = ExceptionTools
        .wrapUnfriendlyExceptions("Something went wrong when looking up the track", FAULT, throwable);

    ExceptionTools.log(log, exception, "loading item " + request.name());

    request.getResponseHandler().loadFailed(exception);
  }

  protected boolean checkSourcesForItem(AudioInfoRequest request, LookupContext context) {
    AudioInfoRequest currentRequest = request;

    for (int redirects = 0; redirects < MAXIMUM_LOAD_REDIRECTS; redirects++) {
      AudioInfoEntity item = checkSourcesForItemOnce(context, currentRequest);

      if (item == null) {
        return false;
      } else if (!(item instanceof AudioInfoRequest)) {
        return true;
      }

      currentRequest = (AudioInfoRequest) item;
    }

    return false;
  }

  protected AudioInfoEntity checkSourcesForItemOnce(LookupContext context, AudioInfoRequest request) {
    for (AudioSource source : sourceRegistry.getAllSources()) {
      if (!request.isSourceAllowed(source)) {
        continue;
      }

      AudioInfoEntity item = source.loadItem(request);

      if (item != null) {
        if (item instanceof AudioTrackInfo) {
          log.debug("Loaded a track with identifier {} using {}.", request.name(), source.getName());
          context.reported = true;
          context.responseHandler.trackLoaded((AudioTrackInfo) item);
        } else if (item instanceof AudioPlaylist) {
          log.debug("Loaded a playlist with identifier {} using {}.", request.name(), source.getName());
          context.reported = true;
          context.responseHandler.playlistLoaded((AudioPlaylist) item);
        }

        return item;
      }
    }

    return null;
  }

  private static class LookupContext {
    private final AudioInfoResponseHandler responseHandler;
    private boolean reported;

    private LookupContext(AudioInfoResponseHandler responseHandler) {
      this.responseHandler = responseHandler;
    }
  }
}
