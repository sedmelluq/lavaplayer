package com.sedmelluq.lavaplayer.core.source.http;

import com.sedmelluq.lavaplayer.core.container.MediaContainerDetection;
import com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult;
import com.sedmelluq.lavaplayer.core.container.MediaContainerHints;
import com.sedmelluq.lavaplayer.core.container.MediaContainerRegistry;
import com.sedmelluq.lavaplayer.core.http.HttpClientTools;
import com.sedmelluq.lavaplayer.core.http.HttpConfigurable;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.http.HttpInterfaceManager;
import com.sedmelluq.lavaplayer.core.http.PersistentHttpStream;
import com.sedmelluq.lavaplayer.core.http.ThreadLocalHttpInterfaceManager;
import com.sedmelluq.lavaplayer.core.info.AudioInfoEntity;
import com.sedmelluq.lavaplayer.core.info.loader.AudioInfoRequests;
import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.request.generic.GenericAudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.source.AbstractProtocolAudioSource;
import com.sedmelluq.lavaplayer.core.source.AudioSource;
import com.sedmelluq.lavaplayer.core.source.ProtocolAudioTrackInfo;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;

import static com.sedmelluq.lavaplayer.core.container.MediaContainerDetectionResult.refer;
import static com.sedmelluq.lavaplayer.core.http.HttpClientTools.getHeaderValue;
import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.COMMON;
import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager which implements finding audio files from HTTP addresses.
 */
public class HttpAudioSource extends AbstractProtocolAudioSource implements HttpConfigurable {
  private static final Set<Class<? extends AudioSource>> SELF_SINGLETON_SET =
      Collections.singleton(HttpAudioSource.class);

  private final HttpInterfaceManager httpInterfaceManager;

  /**
   * Create a new instance with default media container registry.
   */
  public HttpAudioSource() {
    this(MediaContainerRegistry.DEFAULT_REGISTRY);
  }

  /**
   * Create a new instance.
   */
  public HttpAudioSource(MediaContainerRegistry containerRegistry) {
    super(containerRegistry);

    httpInterfaceManager = new ThreadLocalHttpInterfaceManager(
        HttpClientTools
            .createSharedCookiesHttpBuilder()
            .setRedirectStrategy(new HttpClientTools.NoRedirectsStrategy()),
        HttpClientTools.DEFAULT_REQUEST_CONFIG
    );
  }

  @Override
  public String getName() {
    return "http";
  }

  @Override
  public AudioInfoEntity loadItem(AudioInfoRequest request) {
    String url = extractUrl(request);

    if (url != null) {
      return handleLoadResult(url, detectContainer(request, url));
    } else {
      return null;
    }
  }

  @Override
  public AudioTrackInfo decorateTrackInfo(AudioTrackInfo trackInfo) {
    return new ProtocolAudioTrackInfo(trackInfo);
  }

  @Override
  public AudioPlayback createPlayback(AudioTrackInfo trackInfo) {
    return new HttpUrlPlayback(trackInfo, detectProbe(trackInfo), httpInterfaceManager);
  }

  /**
   * @return Get an HTTP interface for a playing track.
   */
  public HttpInterface getHttpInterface() {
    return httpInterfaceManager.getInterface();
  }

  @Override
  public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
    httpInterfaceManager.configureRequests(configurator);
  }

  @Override
  public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
    httpInterfaceManager.configureBuilder(configurator);
  }

  public static String extractUrl(AudioInfoRequest request) {
    if (request instanceof GenericAudioInfoRequest) {
      String hint = ((GenericAudioInfoRequest) request).getHint();

      if (hint.startsWith("https://") || hint.startsWith("http://")) {
        return hint;
      } else if (hint.startsWith("icy://")) {
        return "http://" + hint.substring(6);
      }
    }

    return null;
  }

  private MediaContainerDetectionResult detectContainer(AudioInfoRequest request, String url) {
    MediaContainerDetectionResult result;

    try (HttpInterface httpInterface = getHttpInterface()) {
      result = detectContainerWithClient(httpInterface, request, url);
    } catch (IOException e) {
      throw new FriendlyException("Connecting to the URL failed.", SUSPICIOUS, e);
    }

    return result;
  }

  private MediaContainerDetectionResult detectContainerWithClient(
      HttpInterface httpInterface,
      AudioInfoRequest request,
      String url
  ) throws IOException {
    try (PersistentHttpStream inputStream = new PersistentHttpStream(httpInterface, URI.create(url), Long.MAX_VALUE)) {
      int statusCode = inputStream.checkStatusCode();
      String redirectUrl = HttpClientTools.getRedirectLocation(url, inputStream.getCurrentResponse());

      if (redirectUrl != null) {
        return refer(null, AudioInfoRequests.genericBuilder(redirectUrl)
            .withAllowedSources(SELF_SINGLETON_SET)
            .withInheritedFields(request)
            .build());
      } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
        return null;
      } else if (!HttpClientTools.isSuccessWithContent(statusCode)) {
        throw new FriendlyException("That URL is not playable.", COMMON,
            new IllegalStateException("Status code " + statusCode));
      }

      MediaContainerHints hints = MediaContainerHints.from(
          getHeaderValue(inputStream.getCurrentResponse(), "Content-Type"),
          null
      );

      return new MediaContainerDetection(containerRegistry, request, inputStream, hints).detectContainer();
    }
  }

  @Override
  public void close() {
    ExceptionTools.closeWithWarnings(httpInterfaceManager);
  }
}
