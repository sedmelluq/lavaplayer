package com.sedmelluq.discord.lavaplayer.source.beam;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Dead site, class kept for now to not cause a breaking change.
 */
public class BeamAudioSourceManager implements AudioSourceManager, HttpConfigurable {
  @Override
  public String getSourceName() {
    return "beam.pro";
  }

  @Override
  public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
    return null;
  }

  @Override
  public boolean isTrackEncodable(AudioTrack track) {
    return true;
  }

  @Override
  public void encodeTrack(AudioTrack track, DataOutput output) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
    throw new UnsupportedEncodingException();
  }

  @Override
  public void shutdown() {
    // Nothing to do
  }

  @Override
  public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
    // Nothing to do
  }

  @Override
  public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
    // Nothing to do
  }
}
