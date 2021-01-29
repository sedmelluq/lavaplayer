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

  private static final int DEFAULT_LOADER_POOL_SIZE = 10;
  private static final int LOADER_QUEUE_CAPACITY = 5000;

  private final ThreadPoolExecutor trackInfoExecutorService;
  private final OrderedExecutor orderedInfoExecutor;
  private final SynchronousAudioInfoRequestHandler synchronousHandler;

  public DefaultAudioInfoRequestHandler(AudioSourceRegistry sourceRegistry) {
    trackInfoExecutorService = ExecutorTools.createEagerlyScalingExecutor(1, DEFAULT_LOADER_POOL_SIZE,
        TimeUnit.SECONDS.toMillis(30), LOADER_QUEUE_CAPACITY, new DaemonThreadFactory("info-loader"));
    orderedInfoExecutor = new OrderedExecutor(trackInfoExecutorService);
    synchronousHandler = new SynchronousAudioInfoRequestHandler(sourceRegistry);
  }

  @Override
  public Future<Void> request(AudioInfoRequest request) {
    try {
      Callable<Void> loader = () -> {
        synchronousHandler.processRequest(request);
        return null;
      };

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
}
