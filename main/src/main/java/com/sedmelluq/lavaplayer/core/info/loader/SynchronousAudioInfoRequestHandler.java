package com.sedmelluq.lavaplayer.core.info.loader;

import com.sedmelluq.lavaplayer.core.info.AudioInfoEntity;
import com.sedmelluq.lavaplayer.core.info.playlist.AudioPlaylist;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.source.AudioSource;
import com.sedmelluq.lavaplayer.core.source.AudioSourceRegistry;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.FAULT;

public class SynchronousAudioInfoRequestHandler implements AudioInfoRequestHandler {
  private static final Logger log = LoggerFactory.getLogger(SynchronousAudioInfoRequestHandler.class);

  private static final int MAXIMUM_LOAD_REDIRECTS = 5;

  protected final AudioSourceRegistry sourceRegistry;

  public SynchronousAudioInfoRequestHandler(AudioSourceRegistry sourceRegistry) {
    this.sourceRegistry = sourceRegistry;
  }

  @Override
  public Future<Void> request(AudioInfoRequest request) {
    processRequest(request);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void close() throws Exception {
    // Nothing to do
  }

  public void processRequest(AudioInfoRequest request) {
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

  protected void dispatchItemLoadFailure(AudioInfoRequest request, Throwable throwable) {
    FriendlyException exception = ExceptionTools
        .wrapUnfriendlyExceptions("Something went wrong when looking up the track", FAULT, throwable);

    ExceptionTools.log(log, exception, "loading item " + request.name());

    request.getResponseHandler().loadFailed(exception);
  }

  protected static class LookupContext {
    private final AudioInfoResponseHandler responseHandler;
    private boolean reported;

    private LookupContext(AudioInfoResponseHandler responseHandler) {
      this.responseHandler = responseHandler;
    }
  }
}
