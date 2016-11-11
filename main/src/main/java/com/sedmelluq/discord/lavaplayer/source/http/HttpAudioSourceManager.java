package com.sedmelluq.discord.lavaplayer.source.http;

import com.sedmelluq.discord.lavaplayer.container.MediaContainer;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerProbe;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.ProbingAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager which implements finding audio files from HTTP addresses.
 */
public class HttpAudioSourceManager extends ProbingAudioSourceManager {
  private final HttpClientBuilder httpClientBuilder;

  /**
   * Create a new instance.
   */
  public HttpAudioSourceManager() {
    httpClientBuilder = HttpClientTools.createSharedCookiesHttpBuilder();
  }

  /**
   * @return A new HttpClient instance.
   */
  public CloseableHttpClient createHttpClient() {
    return httpClientBuilder.build();
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

    try (CloseableHttpClient httpClient = createHttpClient()) {
      result = detectContainerWithClient(httpClient, reference);
    } catch (IOException e) {
      throw new FriendlyException("Connecting to the URL failed.", SUSPICIOUS, e);
    }

    return result;
  }

  private MediaContainerDetectionResult detectContainerWithClient(CloseableHttpClient httpClient, AudioReference reference) throws IOException {
    try (PersistentHttpStream inputStream = new PersistentHttpStream(httpClient, new URI(reference.identifier), Long.MAX_VALUE)) {
      int statusCode = inputStream.checkStatusCode();

      if (statusCode == 404) {
        return null;
      } else if (statusCode != 200 && statusCode != 206) {
        throw new FriendlyException("That URL is not playable.", COMMON, new IllegalStateException("Status code " + statusCode));
      }

      return MediaContainerDetection.detectContainer(reference, inputStream);
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
