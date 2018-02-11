package com.sedmelluq.discord.lavaplayer.source.http;

import com.sedmelluq.discord.lavaplayer.container.MediaContainer;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerProbe;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.ProbingAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.tools.io.ThreadLocalHttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;
import static com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools.getHeaderValue;

/**
 * Audio source manager which implements finding audio files from HTTP addresses.
 */
public class HttpAudioSourceManager extends ProbingAudioSourceManager implements HttpConfigurable {
  private final HttpInterfaceManager httpInterfaceManager;

  /**
   * Create a new instance.
   */
  public HttpAudioSourceManager() {
    httpInterfaceManager = new ThreadLocalHttpInterfaceManager(
        HttpClientTools
            .createSharedCookiesHttpBuilder()
            .setRedirectStrategy(new HttpClientTools.NoRedirectsStrategy()),
        HttpClientTools.DEFAULT_REQUEST_CONFIG
    );
  }

  @Override
  public String getSourceName() {
    return "http";
  }

  @Override
  public AudioItem loadItem(DefaultAudioPlayerManager manager, AudioReference reference) {
    AudioReference httpReference = getAsHttpReference(reference);
    if (httpReference == null) {
      return null;
    }

    return handleLoadResult(detectContainer(httpReference));
  }

  @Override
  protected AudioTrack createTrack(AudioTrackInfo trackInfo, MediaContainerProbe probe) {
    return new HttpAudioTrack(trackInfo, probe, this);
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

  private AudioReference getAsHttpReference(AudioReference reference) {
    if (reference.identifier.startsWith("https://") || reference.identifier.startsWith("http://")) {
      return reference;
    } else if (reference.identifier.startsWith("icy://")) {
      return new AudioReference("http://" + reference.identifier.substring(6), reference.title);
    }
    return null;
  }

  private MediaContainerDetectionResult detectContainer(AudioReference reference) {
    MediaContainerDetectionResult result;

    try (HttpInterface httpInterface = getHttpInterface()) {
      result = detectContainerWithClient(httpInterface, reference);
    } catch (IOException e) {
      throw new FriendlyException("Connecting to the URL failed.", SUSPICIOUS, e);
    }

    return result;
  }

  private MediaContainerDetectionResult detectContainerWithClient(HttpInterface httpInterface, AudioReference reference) throws IOException {
    try (PersistentHttpStream inputStream = new PersistentHttpStream(httpInterface, new URI(reference.identifier), Long.MAX_VALUE)) {
      int statusCode = inputStream.checkStatusCode();
      String redirectUrl = HttpClientTools.getRedirectLocation(reference.identifier, inputStream.getCurrentResponse());

      if (redirectUrl != null) {
        return new MediaContainerDetectionResult(null, new AudioReference(redirectUrl, null));
      } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
        return null;
      } else if (!HttpClientTools.isSuccessWithContent(statusCode)) {
        throw new FriendlyException("That URL is not playable.", COMMON, new IllegalStateException("Status code " + statusCode));
      }

      MediaContainerHints hints = MediaContainerHints.from(getHeaderValue(inputStream.getCurrentResponse(), "Content-Type"), null);
      return MediaContainerDetection.detectContainer(reference, inputStream, hints);
    } catch (URISyntaxException e) {
      throw new FriendlyException("Not a valid URL.", COMMON, e);
    }
  }

  @Override
  public boolean isTrackEncodable(AudioTrack track) {
    return true;
  }

  @Override
  public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
    output.writeUTF(((HttpAudioTrack) track).getProbe().getName());
  }

  @Override
  public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
    String probeName = input.readUTF();

    for (MediaContainer container : MediaContainer.class.getEnumConstants()) {
      if (container.probe.getName().equals(probeName)) {
        return new HttpAudioTrack(trackInfo, container.probe, this);
      }
    }

    return null;
  }

  @Override
  public void shutdown() {
    // Nothing to shut down
  }
}
