package com.sedmelluq.discord.lavaplayer.source.http;

import com.sedmelluq.discord.lavaplayer.container.MediaContainer;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
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
public class HttpAudioSourceManager implements AudioSourceManager {
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
  public AudioItem loadItem(DefaultAudioPlayerManager manager, String identifier) {
    if (!identifier.startsWith("https://") && !identifier.startsWith("http://")) {
      return null;
    }

    MediaContainerDetection.Result result = detectContainer(identifier);
    if (result == null) {
      return null;
    }

    return new HttpAudioTrack(result.getTrackInfo(), result.getContainerProbe(), this);
  }

  private MediaContainerDetection.Result detectContainer(String identifier) {
    MediaContainerDetection.Result result;

    try (CloseableHttpClient httpClient = createHttpClient()) {
      result = detectContainerWithClient(httpClient, identifier);
    } catch (IOException e) {
      throw new FriendlyException("Connecting to the URL failed.", SUSPICIOUS, e);
    }

    if (result != null) {
      if (!result.isContainerDetected()) {
        throw new FriendlyException("Unknown file format.", COMMON, null);
      } else if (!result.isSupportedFile()) {
        throw new FriendlyException(result.getUnsupportedReason(), COMMON, null);
      }
    }

    return result;
  }

  private MediaContainerDetection.Result detectContainerWithClient(CloseableHttpClient httpClient, String identifier) throws IOException {
    try (PersistentHttpStream inputStream = new PersistentHttpStream(httpClient, new URI(identifier), Long.MAX_VALUE)) {
      int statusCode = inputStream.checkStatusCode();

      if (statusCode == 404) {
        return null;
      } else if (statusCode != 200 && statusCode != 206) {
        throw new FriendlyException("That URL is not playable.", COMMON, new IllegalStateException("Status code " + statusCode));
      }

      return MediaContainerDetection.detectContainer(identifier, inputStream);
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
