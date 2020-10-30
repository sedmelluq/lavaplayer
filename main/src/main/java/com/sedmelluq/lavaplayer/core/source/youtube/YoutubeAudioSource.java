package com.sedmelluq.lavaplayer.core.source.youtube;

import com.sedmelluq.lavaplayer.core.http.ExtendedHttpConfigurable;
import com.sedmelluq.lavaplayer.core.http.HttpClientTools;
import com.sedmelluq.lavaplayer.core.http.HttpConfigurable;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.HttpInterfaceManager;
import com.sedmelluq.lavaplayer.core.http.MultiHttpConfigurable;
import com.sedmelluq.lavaplayer.core.info.AudioInfoEntity;
import com.sedmelluq.lavaplayer.core.info.loader.AudioInfoRequests;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.request.generic.GenericAudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoTemplate;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.source.AudioSource;
import com.sedmelluq.lavaplayer.core.source.youtube.request.YoutubeSearchRequest;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.COMMON;
import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.FAULT;
import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager that implements finding Youtube videos or playlists based on an URL or ID.
 */
public class YoutubeAudioSource implements AudioSource, HttpConfigurable {
  private static final Logger log = LoggerFactory.getLogger(YoutubeAudioSource.class);

  private static final Set<Class<? extends AudioSource>> SELF_SINGLETON_SET =
      Collections.singleton(YoutubeAudioSource.class);

  private final YoutubeSignatureResolver signatureResolver;
  private final HttpInterfaceManager httpInterfaceManager;
  private final ExtendedHttpConfigurable combinedHttpConfiguration;
  private final DefaultYoutubeMixLoader mixProvider;
  private final YoutubeTrackDetailsLoader trackDetailsLoader;
  private final YoutubeSearchResultLoader searchResultLoader;
  private final YoutubePlaylistLoader playlistLoader;
  private final YoutubeLinkRouter linkRouter;

  public static YoutubeAudioSource createDefault() {
    return new YoutubeAudioSource(
        new DefaultYoutubeTrackDetailsLoader(),
        new DefaultYoutubeSearchResultLoader(),
        new DefaultYoutubeSignatureResolver(),
        new DefaultYoutubePlaylistLoader(),
        new DefaultYoutubeMixLoader(),
        new DefaultYoutubeLinkRouter()
    );
  }

  public YoutubeAudioSource(
      YoutubeTrackDetailsLoader trackDetailsLoader,
      YoutubeSearchResultLoader searchResultLoader,
      YoutubeSignatureResolver signatureResolver,
      YoutubePlaylistLoader playlistLoader,
      DefaultYoutubeMixLoader mixProvider,
      YoutubeLinkRouter linkRouter
  ) {
    httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
    httpInterfaceManager.setHttpContextFilter(new YoutubeHttpContextFilter());

    this.trackDetailsLoader = trackDetailsLoader;
    this.signatureResolver = signatureResolver;
    this.searchResultLoader = searchResultLoader;
    this.playlistLoader = playlistLoader;
    this.mixProvider = mixProvider;
    this.linkRouter = linkRouter;

    combinedHttpConfiguration = new MultiHttpConfigurable(Arrays.asList(
        httpInterfaceManager,
        searchResultLoader.getHttpConfiguration()
    ));
  }

  public YoutubeTrackDetailsLoader getTrackDetailsLoader() {
    return trackDetailsLoader;
  }

  public YoutubeSignatureResolver getSignatureResolver() {
    return signatureResolver;
  }

  public DefaultYoutubeMixLoader getMixProvider() {
    return mixProvider;
  }

  /**
   * @param playlistPageCount Maximum number of pages loaded from one playlist. There are 100 tracks per page.
   */
  public void setPlaylistPageCount(int playlistPageCount) {
    playlistLoader.setPlaylistPageCount(playlistPageCount);
  }

  @Override
  public String getName() {
    return "youtube";
  }

  @Override
  public AudioInfoEntity loadItem(AudioInfoRequest request) {
    try {
      return loadItemOnce(request);
    } catch (FriendlyException exception) {
      // In case of a connection reset exception, try once more.
      if (HttpClientTools.isRetriableNetworkException(exception.getCause())) {
        return loadItemOnce(request);
      } else {
        throw exception;
      }
    }
  }

  @Override
  public AudioTrackInfo decorateTrackInfo(AudioTrackInfo trackInfo) {
    if (trackInfo instanceof YoutubeAudioTrackInfo) {
      return trackInfo;
    } else {
      return new YoutubeAudioTrackInfo(trackInfo);
    }
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo) {
    return new YoutubeUrlPlayback(this, trackInfo);
  }

  @Override
  public void close() throws Exception {
    ExceptionTools.closeWithWarnings(httpInterfaceManager);

    mixProvider.close();
  }

  /**
   * @return Get an HTTP interface for a playing track.
   */
  public HttpInterface getHttpInterface() {
    return httpInterfaceManager.getInterface();
  }

  @Override
  public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
    combinedHttpConfiguration.configureRequests(configurator);
  }

  @Override
  public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
    combinedHttpConfiguration.configureBuilder(configurator);
  }

  public ExtendedHttpConfigurable getHttpConfiguration() {
    return combinedHttpConfiguration;
  }

  public ExtendedHttpConfigurable getMainHttpConfiguration() {
    return httpInterfaceManager;
  }

  public ExtendedHttpConfigurable getSearchHttpConfiguration() {
    return searchResultLoader.getHttpConfiguration();
  }

  private AudioInfoEntity loadItemOnce(AudioInfoRequest request) {
    if (request instanceof GenericAudioInfoRequest) {
      return linkRouter.route(((GenericAudioInfoRequest) request).getHint(), new LoadingRoutes(request));
    } else if (request instanceof YoutubeSearchRequest) {
      return searchResultLoader.loadSearchResult(((YoutubeSearchRequest) request).getSearchQuery(), request);
    } else {
      return null;
    }
  }

  /**
   * Loads a single track from video ID.
   *
   * @param videoId ID of the YouTube video.
   * @param mustExist True if it should throw an exception on missing track, otherwise returns AudioReference.NO_TRACK.
   * @return Loaded YouTube track.
   */
  public AudioInfoEntity loadTrackWithVideoId(String videoId, boolean mustExist, AudioTrackInfoTemplate template) {
    try (HttpInterface httpInterface = getHttpInterface()) {
      YoutubeTrackDetails details = trackDetailsLoader.loadDetails(httpInterface, videoId, template, false);

      if (details == null) {
        if (mustExist) {
          throw new FriendlyException("Video unavailable", COMMON, null);
        } else {
          return AudioInfoEntity.NO_INFO;
        }
      }

      return details.getTrackInfo();
    } catch (Exception e) {
      throw ExceptionTools.wrapUnfriendlyExceptions("Loading information for a YouTube track failed.", FAULT, e);
    }
  }

  private class LoadingRoutes implements YoutubeLinkRouter.Routes<AudioInfoEntity> {
    private final AudioInfoRequest request;

    private LoadingRoutes(AudioInfoRequest request) {
      this.request = request;
    }

    @Override
    public AudioInfoEntity track(String videoId) {
      return loadTrackWithVideoId(videoId, false, request);
    }

    @Override
    public AudioInfoEntity playlist(String playlistId, String selectedVideoId) {
      log.debug("Starting to load playlist with ID {}", playlistId);

      try (HttpInterface httpInterface = getHttpInterface()) {
        return playlistLoader.load(httpInterface, playlistId, selectedVideoId, request);
      } catch (Exception e) {
        throw ExceptionTools.wrapUnfriendlyExceptions(e);
      }
    }

    @Override
    public AudioInfoEntity mix(String mixId, String selectedVideoId) {
      return mixProvider.loadMixWithId(httpInterfaceManager, mixId, selectedVideoId, request);
    }

    @Override
    public AudioInfoEntity search(String query) {
      return searchResultLoader.loadSearchResult(query, request);
    }

    @Override
    public AudioInfoEntity anonymous(String videoIds) {
      try (HttpInterface httpInterface = getHttpInterface()) {
        try (CloseableHttpResponse response = httpInterface.execute(new HttpGet("https://www.youtube.com/watch_videos?video_ids=" + videoIds))) {
          int statusCode = response.getStatusLine().getStatusCode();
          HttpClientContext context = httpInterface.getContext();
          if (!HttpClientTools.isSuccessWithContent(statusCode)) {
            throw new IOException("Invalid status code for playlist response: " + statusCode);
          }
          // youtube currently transforms watch_video links into a link with a video id and a list id.
          // because thats what happens, we can simply re-process with the redirected link
          List<URI> redirects = context.getRedirectLocations();
          if (redirects != null && !redirects.isEmpty()) {
            return AudioInfoRequests
                .genericBuilder(redirects.get(0).toString())
                .withInheritedFields(request)
                .withAllowedSources(SELF_SINGLETON_SET)
                .build();
          } else {
            throw new FriendlyException("Unable to process youtube watch_videos link", SUSPICIOUS,
                new IllegalStateException("Expected youtube to redirect watch_videos link to a watch?v={id}&list={list_id} link, but it did not redirect at all"));
          }
        }
      } catch (Exception e) {
        throw ExceptionTools.wrapUnfriendlyExceptions(e);
      }
    }

    @Override
    public AudioInfoEntity none() {
      return AudioInfoEntity.NO_INFO;
    }
  }
}
